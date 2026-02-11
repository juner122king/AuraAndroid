package com.aura.football.domain.model

import java.time.LocalDate

/**
 * 时间轴的一个日期分组
 * 用于在时间轴视图中按日期组织比赛
 */
data class TimelineSection(
    val date: LocalDate,
    val displayText: String,  // "今天 周一", "明天 周二", "12月15日 周三"
    val matches: List<Match>,
    val isToday: Boolean = false
)
