package com.aura.football.domain.usecase

import com.aura.football.domain.repository.LeagueWithTeams
import com.aura.football.domain.repository.TeamRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取所有球队列表（按联赛分组）
 * 使用TeamRepository专门的接口获取数据
 */
class GetAllTeamsUseCase @Inject constructor(
    private val teamRepository: TeamRepository
) {
    operator fun invoke(): Flow<List<LeagueWithTeams>> {
        return teamRepository.getAllTeamsGroupedByLeague()
    }
}
