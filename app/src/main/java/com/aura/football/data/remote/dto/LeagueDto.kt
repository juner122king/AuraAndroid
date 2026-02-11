package com.aura.football.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LeagueDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("country")
    val country: String?,
    @SerializedName("logo_url")
    val logoUrl: String?
)
