package com.aura.football.data.local.entity

import com.aura.football.data.remote.dto.*
import com.aura.football.domain.model.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// DTO to Entity
fun MatchDto.toEntity(): MatchEntity {
    return MatchEntity(
        id = id,
        leagueId = leagueId,
        homeTeamId = homeTeamId,
        awayTeamId = awayTeamId,
        matchTime = matchTime,
        status = status,
        homeScore = homeScore,
        awayScore = awayScore,
        round = round,
        roundNumber = roundNumber
    )
}

fun TeamDto.toEntity(): TeamEntity {
    return TeamEntity(
        id = id,
        name = name,
        logoUrl = logoUrl,
        shortName = shortName,
        nameZh = nameZh,
        shortNameZh = shortNameZh
    )
}

fun LeagueDto.toEntity(): LeagueEntity {
    return LeagueEntity(
        id = id,
        name = name,
        country = country,
        emblemUrl = emblemUrl
    )
}

fun PredictionDto.toEntity(): PredictionEntity {
    return PredictionEntity(
        matchId = matchId,
        modelVersion = modelVersion ?: "v1.0",
        homeWinProb = homeWinProb,
        drawProb = drawProb,
        awayWinProb = awayWinProb,
        confidence = confidence ?: 0.5f,
        explanation = predictionExplanations?.firstOrNull()?.explanation ?: "暂无分析"
    )
}

// Entity to Domain
fun MatchWithRelations.toDomain(): Match {
    return Match(
        id = match.id,
        homeTeam = homeTeam.toDomain(),
        awayTeam = awayTeam.toDomain(),
        league = league.toDomain(),
        matchTime = parseDateTime(match.matchTime),
        status = MatchStatus.fromString(match.status),
        score = if (match.homeScore != null && match.awayScore != null) {
            Score(match.homeScore, match.awayScore)
        } else null,
        prediction = prediction?.toDomain(),
        round = match.round,
        roundNumber = match.roundNumber
    )
}

fun TeamEntity.toDomain(): Team {
    return Team(
        id = id,
        name = name,
        logoUrl = logoUrl,
        shortName = shortName,
        nameZh = nameZh,
        shortNameZh = shortNameZh
    )
}

fun LeagueEntity.toDomain(): League {
    return League(
        id = id,
        name = name,
        country = country,
        emblemUrl = emblemUrl
    )
}

fun PredictionEntity.toDomain(): Prediction {
    return Prediction(
        homeWinProb = homeWinProb,
        drawProb = drawProb,
        awayWinProb = awayWinProb,
        confidence = confidence,
        explanation = explanation,
        modelVersion = modelVersion
    )
}

/**
 * 将UTC时间字符串转换为本地时间
 * 支持多种ISO格式：
 * - 2026-02-06T19:00:00Z
 * - 2026-02-06T19:00:00+00:00
 * - 2026-02-06T19:00:00
 */
private fun parseDateTime(dateString: String): LocalDateTime {
    return try {
        val instant = when {
            // 格式1: 2026-02-06T19:00:00Z
            dateString.endsWith("Z") -> {
                Instant.parse(dateString)
            }
            // 格式2: 2026-02-06T19:00:00+00:00 或其他时区偏移
            dateString.contains("+") || dateString.lastIndexOf("-") > 10 -> {
                // 使用OffsetDateTime解析带时区偏移的时间
                java.time.OffsetDateTime.parse(dateString).toInstant()
            }
            // 格式3: 2026-02-06T19:00:00 (假设是UTC)
            else -> {
                Instant.parse("${dateString}Z")
            }
        }

        // 转换为系统默认时区的本地时间
        val localTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        android.util.Log.d("TimeConversion", "UTC: $dateString -> Local: $localTime (Zone: ${ZoneId.systemDefault()})")
        localTime
    } catch (e: Exception) {
        android.util.Log.e("TimeConversion", "Failed to parse datetime: $dateString", e)
        try {
            // 如果上面失败，尝试直接解析为LocalDateTime（假设已经是本地时间）
            LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e2: Exception) {
            android.util.Log.e("TimeConversion", "Failed to parse as LocalDateTime", e2)
            // 如果都失败，返回当前时间
            LocalDateTime.now()
        }
    }
}
