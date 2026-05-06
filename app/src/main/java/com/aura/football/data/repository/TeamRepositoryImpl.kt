package com.aura.football.data.repository

import com.aura.football.data.local.dao.LeagueDao
import com.aura.football.data.local.dao.LeagueTeamDao
import com.aura.football.data.local.dao.TeamDao
import com.aura.football.data.local.entity.LeagueTeamCrossRef
import com.aura.football.data.local.entity.LeagueTeamRow
import com.aura.football.data.local.entity.toDomain
import com.aura.football.data.local.entity.toEntity
import com.aura.football.data.remote.SupabaseApi
import com.aura.football.data.remote.dto.toDomain
import com.aura.football.domain.model.League
import com.aura.football.domain.model.Team
import com.aura.football.domain.repository.LeagueWithTeams
import com.aura.football.domain.repository.TeamRepository
import com.aura.football.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import javax.inject.Inject

class TeamRepositoryImpl @Inject constructor(
    private val api: SupabaseApi,
    private val teamDao: TeamDao,
    private val leagueDao: LeagueDao,
    private val leagueTeamDao: LeagueTeamDao
) : TeamRepository {

    override fun getAllTeamsGroupedByLeague(): Flow<List<LeagueWithTeams>> = flow {
        AppLogger.d(TAG, "开始获取按联赛分组的球队数据")

        try {
            val startDate = LocalDate.now().minusYears(2).toString()
            val endDate = LocalDate.now().plusDays(30).toString()

            val matchesResponse = api.getMatches(
                matchTimeGte = "gte.$startDate",
                matchTimeLte = "lte.$endDate"
            )

            if (matchesResponse.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            val allTeams = api.getTeams()
            val allLeagues = api.getLeagues()

            teamDao.insertTeams(allTeams.map { it.toEntity() })
            leagueDao.insertLeagues(allLeagues.map { it.toEntity() })

            val crossRefs = matchesResponse
                .flatMap { match ->
                    listOf(
                        LeagueTeamCrossRef(match.leagueId, match.homeTeamId),
                        LeagueTeamCrossRef(match.leagueId, match.awayTeamId)
                    )
                }
                .distinctBy { "${it.leagueId}_${it.teamId}" }

            leagueTeamDao.deleteAll()
            leagueTeamDao.insertCrossRefs(crossRefs)

            val leaguesById = allLeagues.associateBy { it.id }
            val teamsById = allTeams.associateBy { it.id }

            val grouped = crossRefs
                .groupBy { it.leagueId }
                .mapNotNull { (leagueId, refs) ->
                    val league = leaguesById[leagueId]?.toDomain() ?: return@mapNotNull null
                    val teams = refs.mapNotNull { ref -> teamsById[ref.teamId]?.toDomain() }
                        .distinctBy { it.id }
                        .sortedBy { it.name }
                    LeagueWithTeams(league = league, teams = teams)
                }
                .sortedBy { it.league.name }

            emit(grouped)
        } catch (e: Exception) {
            AppLogger.w(TAG, "获取球队数据失败，尝试读取缓存", e)
            val cachedRows = leagueTeamDao.getLeagueTeamRows()
            emit(buildLeagueWithTeamsFromCache(cachedRows))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getTeamById(teamId: Long): Team? {
        return try {
            teamDao.getTeamById(teamId)?.toDomain() ?: run {
                val response = api.getTeamById(id = "eq.$teamId")
                response.firstOrNull()?.let { teamDto ->
                    teamDao.insertTeam(teamDto.toEntity())
                    teamDto.toDomain()
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "获取球队 $teamId 失败", e)
            null
        }
    }

    override suspend fun getAllTeams(): List<Team> {
        return try {
            val response = api.getTeams()
            if (response.isNotEmpty()) {
                teamDao.insertTeams(response.map { it.toEntity() })
                response.map { it.toDomain() }
            } else {
                teamDao.getAllTeams().map { it.toDomain() }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "获取所有球队失败，尝试读取缓存", e)
            teamDao.getAllTeams().map { it.toDomain() }
        }
    }

    private fun buildLeagueWithTeamsFromCache(rows: List<LeagueTeamRow>): List<LeagueWithTeams> {
        if (rows.isEmpty()) return emptyList()

        return rows
            .groupBy { it.league.id }
            .map { (_, leagueRows) ->
                val league = leagueRows.first().league.toDomain()
                val teams = leagueRows.map { it.team.toDomain() }
                    .distinctBy { it.id }
                    .sortedBy { it.name }
                LeagueWithTeams(league = league, teams = teams)
            }
            .sortedBy { it.league.name }
    }

    companion object {
        private const val TAG = "TeamRepository"
    }
}
