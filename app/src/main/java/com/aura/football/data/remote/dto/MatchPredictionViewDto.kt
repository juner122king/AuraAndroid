package com.aura.football.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * match_predictions_view 视图的 DTO
 * 聚合了 matches + teams + leagues + match_predictions + prediction_explanations
 */
data class MatchPredictionViewDto(
    // Match fields
    @SerializedName("match_id")
    val matchId: Long,
    @SerializedName("match_time")
    val matchTime: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("home_score")
    val homeScore: Int?,
    @SerializedName("away_score")
    val awayScore: Int?,

    // Home Team fields
    @SerializedName("home_team_id")
    val homeTeamId: Long,
    @SerializedName("home_team_name")
    val homeTeamName: String,
    @SerializedName("home_team_logo_url")
    val homeTeamLogoUrl: String?,

    // Away Team fields
    @SerializedName("away_team_id")
    val awayTeamId: Long,
    @SerializedName("away_team_name")
    val awayTeamName: String,
    @SerializedName("away_team_logo_url")
    val awayTeamLogoUrl: String?,

    // League fields
    @SerializedName("league_id")
    val leagueId: Long,
    @SerializedName("league_name")
    val leagueName: String,
    @SerializedName("league_logo_url")
    val leagueLogoUrl: String?,
    @SerializedName("country")
    val country: String?,

    // Prediction fields (nullable if no prediction)
    @SerializedName("prediction_id")
    val predictionId: Long?,
    @SerializedName("model_version")
    val modelVersion: String?,
    @SerializedName("home_win_prob")
    val homeWinProb: Float?,
    @SerializedName("draw_prob")
    val drawProb: Float?,
    @SerializedName("away_win_prob")
    val awayWinProb: Float?,
    @SerializedName("confidence")
    val confidence: Float?,

    // Prediction explanations (as JSON array string or structured data)
    @SerializedName("explanations")
    val explanations: List<String>?
)
