package com.aura.football.domain.usecase

import com.aura.football.domain.model.Match
import com.aura.football.domain.model.MatchStatus
import com.aura.football.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * 球队统计数据
 */
data class TeamStats(
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val totalMatches: Int
) {
    val goalDifference: Int
        get() = goalsFor - goalsAgainst
}

/**
 * 获取球队整体统计数据
 * 统计所有已完成比赛的胜平负数据
 */
class GetTeamStatsUseCase @Inject constructor(
    private val matchRepository: MatchRepository
) {
    /**
     * @param teamId 球队ID
     * @return 球队统计数据的Flow
     */
    operator fun invoke(teamId: Long): Flow<TeamStats> {
        // 查询过去2年的比赛数据
        val startDate = LocalDate.now().minusYears(2).toString()
        val endDate = LocalDate.now().toString()

        return matchRepository.getMatches(startDate, endDate).map { matches ->
            calculateStats(matches, teamId)
        }
    }

    private fun calculateStats(matches: List<Match>, teamId: Long): TeamStats {
        var wins = 0
        var draws = 0
        var losses = 0
        var goalsFor = 0
        var goalsAgainst = 0

        // 只统计该球队的已完成比赛
        val finishedMatches = matches.filter { match ->
            match.status == MatchStatus.FINISHED &&
            (match.homeTeam.id == teamId || match.awayTeam.id == teamId)
        }

        finishedMatches.forEach { match ->
            val score = match.score ?: return@forEach

            // 判断我方是主场还是客场
            val isHomeTeam = match.homeTeam.id == teamId
            val ourScore = if (isHomeTeam) score.home else score.away
            val theirScore = if (isHomeTeam) score.away else score.home

            // 累计进球和失球
            goalsFor += ourScore
            goalsAgainst += theirScore

            // 判断胜平负
            when {
                ourScore > theirScore -> wins++
                ourScore < theirScore -> losses++
                else -> draws++
            }
        }

        return TeamStats(
            wins = wins,
            draws = draws,
            losses = losses,
            goalsFor = goalsFor,
            goalsAgainst = goalsAgainst,
            totalMatches = finishedMatches.size
        )
    }
}
