package com.aura.football.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 包含完整关联数据的比赛DTO
 * 用于PostgREST的嵌入式查询
 */
data class MatchWithDetailsDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("home_team_id")
    val homeTeamId: Long,
    @SerializedName("away_team_id")
    val awayTeamId: Long,
    @SerializedName("league_id")
    val leagueId: Long,
    @SerializedName("match_time")
    val matchTime: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("home_score")
    val homeScore: Int?,
    @SerializedName("away_score")
    val awayScore: Int?,
    @SerializedName("home_team")
    val homeTeam: TeamDto,
    @SerializedName("away_team")
    val awayTeam: TeamDto,
    @SerializedName("league")
    val league: LeagueDto
)
