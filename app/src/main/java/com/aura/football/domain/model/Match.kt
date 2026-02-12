package com.aura.football.domain.model

import java.time.LocalDateTime

data class Match(
    val id: Long,
    val homeTeam: Team,
    val awayTeam: Team,
    val league: League,
    val matchTime: LocalDateTime,
    val status: MatchStatus,
    val score: Score?,
    val prediction: Prediction?,
    val round: String? = null,
    val roundNumber: Int? = null
)

data class Score(
    val home: Int,
    val away: Int
)

enum class MatchStatus {
    SCHEDULED,
    LIVE,
    FINISHED,
    POSTPONED;

    companion object {
        fun fromString(status: String): MatchStatus {
            return when (status.lowercase()) {
                "live", "in_play" -> LIVE
                "finished", "ft" -> FINISHED
                "postponed", "pst" -> POSTPONED
                else -> SCHEDULED
            }
        }
    }
}
