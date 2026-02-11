package com.aura.football.data.repository

import com.aura.football.data.local.dao.LeagueDao
import com.aura.football.data.local.entity.toDomain
import com.aura.football.data.local.entity.toEntity
import com.aura.football.data.remote.SupabaseApi
import com.aura.football.data.remote.dto.toDomain
import com.aura.football.domain.model.League
import com.aura.football.domain.model.Standing
import com.aura.football.domain.repository.LeagueRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LeagueRepositoryImpl @Inject constructor(
    private val api: SupabaseApi,
    private val leagueDao: LeagueDao
) : LeagueRepository {

    override suspend fun getLeagues(): List<League> {
        return try {
            // Try cache first
            val cached = leagueDao.getAllLeagues()
            if (cached.isNotEmpty()) {
                return cached.map { it.toDomain() }
            }

            // Fetch from network
            val response = api.getLeagues()
            leagueDao.insertLeagues(response.map { it.toEntity() })
            response.map { it.toDomain() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun getStandings(leagueId: Long): Flow<List<Standing>> = flow {
        try {
            val response = api.getStandings(leagueId = "eq.$leagueId")
            emit(response.map { it.toDomain() })
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
}
