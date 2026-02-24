package com.aura.football.data.local.entity

import com.aura.football.data.remote.dto.*
import com.aura.football.domain.model.*
import com.aura.football.util.parseDateTime

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

