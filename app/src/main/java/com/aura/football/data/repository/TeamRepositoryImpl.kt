package com.aura.football.data.repository

import android.util.Log
import com.aura.football.data.local.dao.TeamDao
import com.aura.football.data.local.entity.toDomain
import com.aura.football.data.local.entity.toEntity
import com.aura.football.data.remote.SupabaseApi
import com.aura.football.data.remote.dto.toDomain
import com.aura.football.domain.model.League
import com.aura.football.domain.model.Team
import com.aura.football.domain.repository.LeagueWithTeams
import com.aura.football.domain.repository.TeamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import javax.inject.Inject

class TeamRepositoryImpl @Inject constructor(
    private val api: SupabaseApi,
    private val teamDao: TeamDao
) : TeamRepository {

    override fun getAllTeamsGroupedByLeague(): Flow<List<LeagueWithTeams>> = flow {
        Log.d(TAG, "开始获取按联赛分组的球队数据")

        try {
            // 1. 先尝试从网络获取最新数据
            // 查询过去2年到未来30天的比赛，以获取活跃的球队和联赛
            val startDate = LocalDate.now().minusYears(2).toString()
            val endDate = LocalDate.now().plusDays(30).toString()

            val matchesResponse = api.getMatches(
                matchTimeGte = "gte.$startDate",
                matchTimeLte = "lte.$endDate"
            )

            Log.d(TAG, "获取到 ${matchesResponse.size} 场比赛")

            if (matchesResponse.isNotEmpty()) {
                // 2. 获取所有teams和leagues
                val allTeams = api.getTeams()
                val allLeagues = api.getLeagues()

                Log.d(TAG, "获取到 ${allTeams.size} 支球队，${allLeagues.size} 个联赛")

                // 3. 缓存到本地
                teamDao.insertTeams(allTeams.map { it.toEntity() })

                // 4. 按联赛分组
                val leagueMap = mutableMapOf<Long, MutableSet<Team>>()
                val leagueInfoMap = mutableMapOf<Long, League>()

                matchesResponse.forEach { match ->
                    val leagueId = match.leagueId

                    // 保存联赛信息
                    if (!leagueInfoMap.containsKey(leagueId)) {
                        allLeagues.find { it.id == leagueId }?.let {
                            leagueInfoMap[leagueId] = it.toDomain()
                        }
                    }

                    // 添加球队到对应联赛
                    if (!leagueMap.containsKey(leagueId)) {
                        leagueMap[leagueId] = mutableSetOf()
                    }

                    // 添加主队和客队
                    allTeams.find { it.id == match.homeTeamId }?.let { team ->
                        leagueMap[leagueId]?.add(team.toDomain())
                    }
                    allTeams.find { it.id == match.awayTeamId }?.let { team ->
                        leagueMap[leagueId]?.add(team.toDomain())
                    }
                }

                // 5. 转换为LeagueWithTeams列表并排序
                val result = leagueMap.mapNotNull { (leagueId, teams) ->
                    leagueInfoMap[leagueId]?.let { league ->
                        LeagueWithTeams(
                            league = league,
                            teams = teams.sortedBy { it.name }
                        )
                    }
                }.sortedBy { it.league.name }

                Log.d(TAG, "成功组织数据：${result.size} 个联赛")
                emit(result)
            } else {
                // 没有比赛数据，返回空列表
                Log.w(TAG, "未获取到比赛数据")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取球队数据失败: ${e.message}", e)

            // 网络失败，尝试从缓存加载
            try {
                val cachedTeams = teamDao.getAllTeams()
                if (cachedTeams.isNotEmpty()) {
                    Log.d(TAG, "从缓存加载 ${cachedTeams.size} 支球队")
                    // 注意：缓存中没有联赛分组信息，这里简化处理
                    // 实际应用中可能需要额外缓存联赛-球队关系
                    emit(emptyList())
                } else {
                    throw e
                }
            } catch (cacheError: Exception) {
                Log.e(TAG, "读取缓存也失败", cacheError)
                throw e
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getTeamById(teamId: Long): Team? {
        return try {
            // 先从缓存查找
            teamDao.getTeamById(teamId)?.toDomain() ?: run {
                // 缓存没有，从网络获取
                val response = api.getTeamById(id = "eq.$teamId")
                response.firstOrNull()?.let { teamDto ->
                    // 缓存到本地
                    teamDao.insertTeam(teamDto.toEntity())
                    teamDto.toDomain()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取球队 $teamId 失败: ${e.message}", e)
            null
        }
    }

    override suspend fun getAllTeams(): List<Team> {
        return try {
            // 先尝试从网络获取
            val response = api.getTeams()
            if (response.isNotEmpty()) {
                // 缓存到本地
                teamDao.insertTeams(response.map { it.toEntity() })
                response.map { it.toDomain() }
            } else {
                // 网络返回空，从缓存读取
                teamDao.getAllTeams().map { it.toDomain() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取所有球队失败: ${e.message}", e)
            // 网络失败，从缓存读取
            teamDao.getAllTeams().map { it.toDomain() }
        }
    }

    companion object {
        private const val TAG = "TeamRepository"
    }
}
