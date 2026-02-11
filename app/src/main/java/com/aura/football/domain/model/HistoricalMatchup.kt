package com.aura.football.domain.model

import java.time.LocalDateTime

data class HistoricalMatchupStats(
    val homeTeamWins: Int,      // Current home team's wins in historical matchups
    val awayTeamWins: Int,      // Current away team's wins in historical matchups
    val draws: Int,
    val homeTeamGoals: Int,     // Total goals scored by current home team
    val awayTeamGoals: Int,     // Total goals scored by current away team
    val totalMatches: Int,
    val matches: List<HistoricalMatch>
)

data class HistoricalMatch(
    val id: Long,
    val matchTime: LocalDateTime,
    val league: League,
    val homeTeam: Team,         // Historical home team (may differ from current match)
    val awayTeam: Team,         // Historical away team (may differ from current match)
    val homeScore: Int,
    val awayScore: Int,
    val status: MatchStatus
)
