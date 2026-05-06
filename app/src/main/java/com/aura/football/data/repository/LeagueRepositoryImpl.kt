package com.aura.football.data.repository

import com.aura.football.data.local.dao.LeagueDao
import com.aura.football.data.local.dao.StandingDao
import com.aura.football.data.local.dao.TeamDao
import com.aura.football.data.local.entity.toDomain
import com.aura.football.data.local.entity.toEntity
import com.aura.football.data.remote.SupabaseApi
import com.aura.football.data.remote.dto.toDomain
import com.aura.football.domain.model.League
import com.aura.football.domain.model.Standing
import com.aura.football.domain.repository.LeagueRepository
import com.aura.football.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LeagueRepositoryImpl @Inject constructor(
    private val api: SupabaseApi,
    private val leagueDao: LeagueDao,
    private val teamDao: TeamDao,
    private val standingDao: StandingDao
) : LeagueRepository {

    @Volatile
    private var lastFetchTime: Long = 0L

    override suspend fun getLeagues(): List<League> {
        return try {
            val cached = leagueDao.getAllLeagues()
            val now = System.currentTimeMillis()
            val cacheExpired = now - lastFetchTime > CACHE_DURATION_MS

            if (cached.isNotEmpty() && !cacheExpired) {
                return cached.map { it.toDomain() }
            }

            try {
                val response = api.getLeagues()
                if (response.isNotEmpty()) {
                    leagueDao.insertLeagues(response.map { it.toEntity() })
                    lastFetchTime = now
                    response.map { it.toDomain() }
                } else if (cached.isNotEmpty()) {
                    cached.map { it.toDomain() }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                if (cached.isNotEmpty()) {
                    cached.map { it.toDomain() }
                } else {
                    throw e
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "加载联赛列表失败", e)
            emptyList()
        }
    }

    override fun getStandings(leagueId: Long): Flow<List<Standing>> = flow {
        try {
            val response = api.getStandings(leagueId = "eq.$leagueId")
            if (response.isNotEmpty()) {
                teamDao.insertTeams(response.map { it.team.toEntity() })
                standingDao.deleteStandingsByLeagueId(leagueId)
                standingDao.insertStandings(response.map { it.toEntity(leagueId) })
            }
            emit(response.map { it.toDomain() })
        } catch (e: Exception) {
            AppLogger.w(TAG, "加载积分榜失败，尝试读取缓存", e)
            val cached = standingDao.getStandingsByLeagueId(leagueId)
            emit(cached.map { it.toDomain() })
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val TAG = "LeagueRepository"
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L
    }
}
