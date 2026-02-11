package com.aura.football.domain.usecase

import com.aura.football.domain.model.Match
import com.aura.football.domain.repository.MatchRepository
import javax.inject.Inject

class GetMatchDetailUseCase @Inject constructor(
    private val repository: MatchRepository
) {
    suspend operator fun invoke(matchId: Long): Match? {
        return repository.getMatchById(matchId)
    }
}
