package com.aura.football.data.remote.dto

import com.aura.football.domain.model.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 转换包含完整关联数据的比赛DTO
 */
fun MatchWithDetailsDto.toDomain(): Match {
    return Match(
        id = id,
        homeTeam = homeTeam.toDomain(),
        awayTeam = awayTeam.toDomain(),
        league = league.toDomain(),
        matchTime = parseDateTime(matchTime),
        status = MatchStatus.fromString(status),
        score = if (homeScore != null && awayScore != null) {
            Score(homeScore, awayScore)
        } else null,
        prediction = null,
        round = round,
        roundNumber = roundNumber
    )
}

// MatchDto.toDomain() 已移除
// 现在在 MatchRepositoryImpl 中直接组合数据
// MatchDto 不再包含嵌套的 team/league 对象

fun TeamDto.toDomain(): Team {
    return Team(
        id = id,
        name = name,
        logoUrl = logoUrl,
        shortName = shortName,
        nameZh = nameZh,
        shortNameZh = shortNameZh
    )
}

fun LeagueDto.toDomain(): League {
    return League(
        id = id,
        name = name,
        country = country,
        logoUrl = logoUrl
    )
}

fun PredictionDto.toDomain(): Prediction {
    return Prediction(
        homeWinProb = homeWinProb,
        drawProb = drawProb,
        awayWinProb = awayWinProb,
        confidence = confidence ?: 0.5f,
        explanation = predictionExplanations?.firstOrNull()?.explanation ?: "暂无分析",
        modelVersion = modelVersion ?: "v1.0"
    )
}

fun StandingDto.toDomain(): Standing {
    return Standing(
        position = position,
        team = team.toDomain(),
        played = played,
        won = won,
        drawn = drawn,
        lost = lost,
        goalsFor = goalsFor,
        goalsAgainst = goalsAgainst,
        goalDifference = goalDifference,
        points = points
    )
}

/**
 * 转换 match_predictions_view 视图数据为 Match 对象（包含预测）
 */
fun MatchPredictionViewDto.toDomain(): Match {
    return Match(
        id = matchId,
        homeTeam = Team(
            id = homeTeamId,
            name = homeTeamName,
            logoUrl = homeTeamLogoUrl,
            shortName = null,
            nameZh = null,
            shortNameZh = null
        ),
        awayTeam = Team(
            id = awayTeamId,
            name = awayTeamName,
            logoUrl = awayTeamLogoUrl,
            shortName = null,
            nameZh = null,
            shortNameZh = null
        ),
        league = League(
            id = leagueId,
            name = leagueName,
            country = country,
            logoUrl = leagueLogoUrl
        ),
        matchTime = parseDateTime(matchTime),
        status = MatchStatus.fromString(status),
        score = if (homeScore != null && awayScore != null) {
            Score(homeScore, awayScore)
        } else null,
        prediction = if (predictionId != null && homeWinProb != null && drawProb != null && awayWinProb != null) {
            Prediction(
                homeWinProb = homeWinProb,
                drawProb = drawProb,
                awayWinProb = awayWinProb,
                confidence = confidence ?: 0.5f,
                explanation = explanations?.joinToString("\n") ?: "暂无分析",
                modelVersion = modelVersion ?: "v1.0"
            )
        } else null,
        round = round,
        roundNumber = roundNumber
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
        instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    } catch (e: Exception) {
        try {
            // 如果上面失败，尝试直接解析为LocalDateTime（假设已经是本地时间）
            LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            // 如果都失败，返回当前时间
            LocalDateTime.now()
        }
    }
}
