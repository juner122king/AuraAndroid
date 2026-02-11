package com.aura.football.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MatchDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("league_id")
    val leagueId: Long,
    @SerializedName("home_team_id")
    val homeTeamId: Long,
    @SerializedName("away_team_id")
    val awayTeamId: Long,
    @SerializedName("match_time")
    val matchTime: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("home_score")
    val homeScore: Int?,
    @SerializedName("away_score")
    val awayScore: Int?
)
