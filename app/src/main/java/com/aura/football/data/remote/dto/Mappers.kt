package com.aura.football.data.remote.dto

import com.aura.football.domain.model.*
import com.aura.football.util.parseDateTime

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
        emblemUrl = emblemUrl
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
            emblemUrl = leagueLogoUrl
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

