package com.aura.football.domain.repository

import com.aura.football.domain.model.HistoricalMatchupStats
import com.aura.football.domain.model.Match
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    fun getMatches(startDate: String, endDate: String): Flow<List<Match>>
    suspend fun getMatchById(matchId: Long): Match?
    suspend fun updateLiveMatches()
    suspend fun getHistoricalMatchups(
        homeTeamId: Long,
        awayTeamId: Long,
        leagueId: Long? = null
    ): HistoricalMatchupStats
}
