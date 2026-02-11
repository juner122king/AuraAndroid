package com.aura.football.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StandingDto(
    @SerializedName("position")
    val position: Int,
    @SerializedName("team_id")
    val teamId: Long,
    @SerializedName("team")
    val team: TeamDto,
    @SerializedName("played")
    val played: Int,
    @SerializedName("won")
    val won: Int,
    @SerializedName("drawn")
    val drawn: Int,
    @SerializedName("lost")
    val lost: Int,
    @SerializedName("goals_for")
    val goalsFor: Int,
    @SerializedName("goals_against")
    val goalsAgainst: Int,
    @SerializedName("goal_difference")
    val goalDifference: Int,
    @SerializedName("points")
    val points: Int
)
