package com.aura.football.domain.model

data class Prediction(
    val homeWinProb: Float,
    val drawProb: Float,
    val awayWinProb: Float,
    val confidence: Float,
    val explanation: String,
    val modelVersion: String
) {
    val mostLikelyOutcome: PredictionOutcome
        get() = when {
            homeWinProb > drawProb && homeWinProb > awayWinProb -> PredictionOutcome.HOME_WIN
            awayWinProb > drawProb && awayWinProb > homeWinProb -> PredictionOutcome.AWAY_WIN
            else -> PredictionOutcome.DRAW
        }
}

enum class PredictionOutcome {
    HOME_WIN,
    DRAW,
    AWAY_WIN
}
