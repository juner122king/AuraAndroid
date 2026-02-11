package com.aura.football.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * get_match_predictions RPC 的参数
 */
data class MatchPredictionsRpcParams(
    @SerializedName("from_ts")
    val fromTs: String,
    @SerializedName("to_ts")
    val toTs: String,
    @SerializedName("league_id_filter")
    val leagueIdFilter: Long? = null,
    @SerializedName("status_filter")
    val statusFilter: String? = null,
    @SerializedName("only_predicted")
    val onlyPredicted: Boolean? = false,
    @SerializedName("limit_count")
    val limitCount: Int? = 50,
    @SerializedName("offset_count")
    val offsetCount: Int? = 0
)
