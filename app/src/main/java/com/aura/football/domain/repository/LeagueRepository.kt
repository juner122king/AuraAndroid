package com.aura.football.domain.repository

import com.aura.football.domain.model.League
import com.aura.football.domain.model.Standing
import kotlinx.coroutines.flow.Flow

interface LeagueRepository {
    suspend fun getLeagues(): List<League>
    fun getStandings(leagueId: Long): Flow<List<Standing>>
}
