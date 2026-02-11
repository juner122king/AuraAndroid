package com.aura.football.domain.usecase

import com.aura.football.domain.model.Standing
import com.aura.football.domain.repository.LeagueRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStandingsUseCase @Inject constructor(
    private val repository: LeagueRepository
) {
    operator fun invoke(leagueId: Long): Flow<List<Standing>> {
        return repository.getStandings(leagueId)
    }
}
