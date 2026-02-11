package com.aura.football.domain.model

data class Team(
    val id: Long,
    val name: String,
    val logoUrl: String?,
    val shortName: String?,
    val nameZh: String?,
    val shortNameZh: String?
) {
    /**
     * 获取显示名称：中文名(英文简称)
     * 例如：乌德勒支(UTR)
     */
    val displayName: String
        get() = when {
            !nameZh.isNullOrBlank() && !shortName.isNullOrBlank() -> "$nameZh($shortName)"
            !nameZh.isNullOrBlank() -> nameZh
            !shortName.isNullOrBlank() -> "$name($shortName)"
            else -> name
        }
}
