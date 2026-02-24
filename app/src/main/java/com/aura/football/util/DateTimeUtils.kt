package com.aura.football.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 将UTC时间字符串转换为本地时间
 * 支持多种ISO格式：
 * - 2026-02-06T19:00:00Z
 * - 2026-02-06T19:00:00+00:00
 * - 2026-02-06T19:00:00
 */
fun parseDateTime(dateString: String): LocalDateTime {
    return try {
        val instant = when {
            dateString.endsWith("Z") -> {
                Instant.parse(dateString)
            }
            dateString.contains("+") || dateString.lastIndexOf("-") > 10 -> {
                OffsetDateTime.parse(dateString).toInstant()
            }
            else -> {
                Instant.parse("${dateString}Z")
            }
        }

        val zoneId = java.util.TimeZone.getDefault().toZoneId()
        instant.atZone(zoneId).toLocalDateTime()
    } catch (e: Exception) {
        try {
            LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e2: Exception) {
            LocalDateTime.now()
        }
    }
}
