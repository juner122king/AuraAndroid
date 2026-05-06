package com.aura.football.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aura.football.domain.model.Match
import com.aura.football.domain.model.MatchStatus
import com.aura.football.domain.model.Score
import java.time.LocalDateTime

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "league_id")
    val leagueId: Long,
    @ColumnInfo(name = "home_team_id")
    val homeTeamId: Long,
    @ColumnInfo(name = "away_team_id")
    val awayTeamId: Long,
    @ColumnInfo(name = "match_time")
    val matchTime: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "home_score")
    val homeScore: Int?,
    @ColumnInfo(name = "away_score")
    val awayScore: Int?,
    @ColumnInfo(name = "round")
    val round: String?,
    @ColumnInfo(name = "round_number")
    val roundNumber: Int?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    @ColumnInfo(name = "logo_url")
    val logoUrl: String?,
    @ColumnInfo(name = "short_name")
    val shortName: String?,
    @ColumnInfo(name = "name_zh")
    val nameZh: String?,
    @ColumnInfo(name = "short_name_zh")
    val shortNameZh: String?
)

@Entity(tableName = "leagues")
data class LeagueEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val country: String?,
    @ColumnInfo(name = "emblem_url")
    val emblemUrl: String?
)

@Entity(tableName = "predictions")
data class PredictionEntity(
    @PrimaryKey
    @ColumnInfo(name = "match_id")
    val matchId: Long,
    @ColumnInfo(name = "model_version")
    val modelVersion: String,
    @ColumnInfo(name = "home_win_prob")
    val homeWinProb: Float,
    @ColumnInfo(name = "draw_prob")
    val drawProb: Float,
    @ColumnInfo(name = "away_win_prob")
    val awayWinProb: Float,
    val confidence: Float,
    val explanation: String
)

@Entity(
    tableName = "standings",
    primaryKeys = ["league_id", "team_id"],
    indices = [Index("league_id"), Index("team_id")]
)
data class StandingEntity(
    @ColumnInfo(name = "league_id")
    val leagueId: Long,
    @ColumnInfo(name = "team_id")
    val teamId: Long,
    val position: Int,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    @ColumnInfo(name = "goals_for")
    val goalsFor: Int,
    @ColumnInfo(name = "goals_against")
    val goalsAgainst: Int,
    @ColumnInfo(name = "goal_difference")
    val goalDifference: Int,
    val points: Int,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "league_team_cross_refs",
    primaryKeys = ["league_id", "team_id"],
    indices = [Index("league_id"), Index("team_id")]
)
data class LeagueTeamCrossRef(
    @ColumnInfo(name = "league_id")
    val leagueId: Long,
    @ColumnInfo(name = "team_id")
    val teamId: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

data class MatchWithRelations(
    @Embedded val match: MatchEntity,
    @Embedded(prefix = "ht_")
    val homeTeam: TeamEntity,
    @Embedded(prefix = "at_")
    val awayTeam: TeamEntity,
    @Embedded(prefix = "lg_")
    val league: LeagueEntity,
    @Embedded(prefix = "pred_")
    val prediction: PredictionEntity?
)

data class StandingWithTeam(
    @Embedded val standing: StandingEntity,
    @Embedded(prefix = "tm_")
    val team: TeamEntity
)

data class LeagueTeamRow(
    @Embedded(prefix = "lg_")
    val league: LeagueEntity,
    @Embedded(prefix = "tm_")
    val team: TeamEntity
)
