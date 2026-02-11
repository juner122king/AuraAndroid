package com.aura.football.domain.usecase

import com.aura.football.domain.model.HistoricalMatchupStats
import com.aura.football.domain.repository.MatchRepository
import javax.inject.Inject

class GetHistoricalMatchupsUseCase @Inject constructor(
    private val repository: MatchRepository
) {
    suspend operator fun invoke(
        homeTeamId: Long,
        awayTeamId: Long,
        leagueId: Long? = null
    ): HistoricalMatchupStats {
        return repository.getHistoricalMatchups(homeTeamId, awayTeamId, leagueId)
    }
}
