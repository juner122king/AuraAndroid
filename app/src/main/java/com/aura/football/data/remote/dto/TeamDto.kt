package com.aura.football.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TeamDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("logo_url")
    val logoUrl: String?,
    @SerializedName("short_name")
    val shortName: String?,
    @SerializedName("name_zh")
    val nameZh: String?,
    @SerializedName("short_name_zh")
    val shortNameZh: String?
)
