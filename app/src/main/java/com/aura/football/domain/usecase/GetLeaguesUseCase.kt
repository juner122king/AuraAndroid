package com.aura.football.domain.usecase

import com.aura.football.domain.model.League
import com.aura.football.domain.repository.LeagueRepository
import javax.inject.Inject

class GetLeaguesUseCase @Inject constructor(
    private val repository: LeagueRepository
) {
    suspend operator fun invoke(): List<League> {
        return repository.getLeagues()
    }
}
