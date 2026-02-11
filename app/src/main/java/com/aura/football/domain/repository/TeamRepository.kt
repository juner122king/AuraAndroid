package com.aura.football.domain.repository

import com.aura.football.domain.model.League
import com.aura.football.domain.model.Team
import kotlinx.coroutines.flow.Flow

/**
 * 按联赛分组的球队数据
 */
data class LeagueWithTeams(
    val league: League,
    val teams: List<Team>
)

/**
 * 球队数据仓库
 */
interface TeamRepository {
    /**
     * 获取所有球队列表（按联赛分组）
     */
    fun getAllTeamsGroupedByLeague(): Flow<List<LeagueWithTeams>>

    /**
     * 获取单个球队信息
     */
    suspend fun getTeamById(teamId: Long): Team?

    /**
     * 获取所有球队
     */
    suspend fun getAllTeams(): List<Team>
}
