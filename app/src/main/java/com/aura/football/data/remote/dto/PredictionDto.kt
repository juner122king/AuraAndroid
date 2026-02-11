package com.aura.football.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PredictionDto(
    @SerializedName("match_id")
    val matchId: Long,
    @SerializedName("model_version")
    val modelVersion: String?,
    @SerializedName("home_win_prob")
    val homeWinProb: Float,
    @SerializedName("draw_prob")
    val drawProb: Float,
    @SerializedName("away_win_prob")
    val awayWinProb: Float,
    @SerializedName("confidence")
    val confidence: Float?,
    @SerializedName("generated_at")
    val generatedAt: String?,
    @SerializedName("prediction_explanations")
    val predictionExplanations: List<ExplanationDto>?
)

data class ExplanationDto(
    @SerializedName("match_id")
    val matchId: Long,
    @SerializedName("explanation")
    val explanation: String,
    @SerializedName("generated_at")
    val generatedAt: String?
)
