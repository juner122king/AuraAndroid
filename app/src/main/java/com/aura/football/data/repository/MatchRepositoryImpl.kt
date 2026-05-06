package com.aura.football.data.repository

import com.aura.football.data.local.dao.LeagueDao
import com.aura.football.data.local.dao.MatchDao
import com.aura.football.data.local.dao.PredictionDao
import com.aura.football.data.local.dao.TeamDao
import com.aura.football.data.local.entity.LeagueEntity
import com.aura.football.data.local.entity.TeamEntity
import com.aura.football.data.local.entity.toDomain
import com.aura.football.data.local.entity.toEntity
import com.aura.football.data.remote.SupabaseApi
import com.aura.football.data.remote.dto.MatchWithDetailsDto
import com.aura.football.data.remote.dto.PredictionDto
import com.aura.football.data.remote.dto.toDomain
import com.aura.football.domain.model.HistoricalMatch
import com.aura.football.domain.model.HistoricalMatchupStats
import com.aura.football.domain.model.Match
import com.aura.football.domain.model.Prediction
import com.aura.football.domain.model.Score
import com.aura.football.domain.model.MatchStatus
import com.aura.football.domain.repository.MatchRepository
import com.aura.football.util.AppLogger
import com.aura.football.util.parseDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MatchRepositoryImpl @Inject constructor(
    private val api: SupabaseApi,
    private val matchDao: MatchDao,
    private val teamDao: TeamDao,
    private val leagueDao: LeagueDao,
    private val predictionDao: PredictionDao
) : MatchRepository {

    override fun getMatches(startDate: String, endDate: String): Flow<List<Match>> = flow {
        emit(fetchMatchesWithFallback(startDate, endDate))
    }.flowOn(Dispatchers.IO)

    override fun getMatchesForTeam(
        teamId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<Match>> = flow {
        emit(fetchMatchesWithFallback(startDate, endDate, teamId))
    }.flowOn(Dispatchers.IO)

    override suspend fun getMatchById(matchId: Long): Match? {
        matchDao.getMatchById(matchId)?.toDomain()?.let { return it }

        try {
            val fromView = api.getMatchPredictionsFromView(
                matchId = "eq.$matchId",
                limit = 1
            ).firstOrNull()?.toDomain()

            if (fromView != null) {
                cacheMatches(listOf(fromView))
                return fromView
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "从聚合视图读取比赛详情失败，回退旧链路", e)
        }

        return try {
            fetchSingleMatchLegacy(matchId)?.also { cacheMatches(listOf(it)) }
        } catch (e: Exception) {
            AppLogger.e(TAG, "获取比赛详情失败", e)
            null
        }
    }

    override suspend fun updateLiveMatches() {
        try {
            val liveMatches = try {
                api.getMatchPredictionsFromView(status = "eq.live")
                    .map { it.toDomain() }
            } catch (e: Exception) {
                AppLogger.w(TAG, "从聚合视图刷新直播比赛失败，回退旧链路", e)
                fetchLegacyMatches(
                    startDate = null,
                    endDate = null,
                    status = "eq.live"
                )
            }

            if (liveMatches.isNotEmpty()) {
                cacheMatches(liveMatches)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "刷新直播比赛失败", e)
        }
    }

    override suspend fun getHistoricalMatchups(
        homeTeamId: Long,
        awayTeamId: Long,
        leagueId: Long?
    ): HistoricalMatchupStats {
        return withContext(Dispatchers.IO) {
            AppLogger.d(TAG, "开始获取历史对局: $homeTeamId vs $awayTeamId (联赛ID: $leagueId)")

            try {
                val orCondition =
                    "(and(home_team_id.eq.$homeTeamId,away_team_id.eq.$awayTeamId),and(home_team_id.eq.$awayTeamId,away_team_id.eq.$homeTeamId))"
                val limit = 20

                val matchesResponse: List<MatchWithDetailsDto> = if (leagueId != null) {
                    val leagueMatches = api.getHistoricalMatchups(
                        orCondition = orCondition,
                        leagueId = "eq.$leagueId",
                        limit = limit
                    )

                    if (leagueMatches.size >= 3) {
                        leagueMatches
                    } else {
                        api.getHistoricalMatchups(
                            orCondition = orCondition,
                            limit = limit
                        )
                    }
                } else {
                    api.getHistoricalMatchups(
                        orCondition = orCondition,
                        limit = limit
                    )
                }

                if (matchesResponse.isEmpty()) {
                    return@withContext HistoricalMatchupStats(
                        homeTeamWins = 0,
                        awayTeamWins = 0,
                        draws = 0,
                        homeTeamGoals = 0,
                        awayTeamGoals = 0,
                        totalMatches = 0,
                        matches = emptyList()
                    )
                }

                val historicalMatches = matchesResponse.mapNotNull { dto ->
                    if (dto.homeScore != null && dto.awayScore != null) {
                        HistoricalMatch(
                            id = dto.id,
                            matchTime = parseDateTime(dto.matchTime),
                            league = dto.league.toDomain(),
                            homeTeam = dto.homeTeam.toDomain(),
                            awayTeam = dto.awayTeam.toDomain(),
                            homeScore = dto.homeScore,
                            awayScore = dto.awayScore,
                            status = MatchStatus.fromString(dto.status)
                        )
                    } else {
                        null
                    }
                }

                var homeWins = 0
                var awayWins = 0
                var draws = 0
                var homeGoals = 0
                var awayGoals = 0

                historicalMatches.forEach { match ->
                    val currentHomeTeamWasHome = match.homeTeam.id == homeTeamId

                    if (currentHomeTeamWasHome) {
                        homeGoals += match.homeScore
                        awayGoals += match.awayScore
                        when {
                            match.homeScore > match.awayScore -> homeWins++
                            match.homeScore < match.awayScore -> awayWins++
                            else -> draws++
                        }
                    } else {
                        homeGoals += match.awayScore
                        awayGoals += match.homeScore
                        when {
                            match.awayScore > match.homeScore -> homeWins++
                            match.awayScore < match.homeScore -> awayWins++
                            else -> draws++
                        }
                    }
                }

                HistoricalMatchupStats(
                    homeTeamWins = homeWins,
                    awayTeamWins = awayWins,
                    draws = draws,
                    homeTeamGoals = homeGoals,
                    awayTeamGoals = awayGoals,
                    totalMatches = historicalMatches.size,
                    matches = historicalMatches
                )
            } catch (e: Exception) {
                AppLogger.e(TAG, "获取历史对局失败", e)
                HistoricalMatchupStats(
                    homeTeamWins = 0,
                    awayTeamWins = 0,
                    draws = 0,
                    homeTeamGoals = 0,
                    awayTeamGoals = 0,
                    totalMatches = 0,
                    matches = emptyList()
                )
            }
        }
    }

    private suspend fun fetchMatchesWithFallback(
        startDate: String,
        endDate: String,
        teamId: Long? = null
    ): List<Match> {
        return try {
            val matches = fetchMatchesFromView(startDate, endDate, teamId)
            cacheMatches(matches)
            matches.sortedBy { it.matchTime }
        } catch (e: Exception) {
            AppLogger.w(TAG, "从聚合视图获取比赛失败，回退本地缓存", e)
            val cachedMatches = getCachedMatches(startDate, endDate, teamId)
            if (cachedMatches.isNotEmpty()) {
                cachedMatches
            } else {
                val legacyMatches = fetchLegacyMatches(startDate, endDate, teamId = teamId)
                cacheMatches(legacyMatches)
                legacyMatches.sortedBy { it.matchTime }
            }
        }
    }

    private suspend fun fetchMatchesFromView(
        startDate: String,
        endDate: String,
        teamId: Long? = null
    ): List<Match> {
        return api.getMatchPredictionsFromView(
            matchTimeGte = "gte.$startDate",
            matchTimeLte = "lte.$endDate",
            orCondition = teamId?.let { "(home_team_id.eq.$it,away_team_id.eq.$it)" }
        ).map { it.toDomain() }
    }

    private suspend fun fetchLegacyMatches(
        startDate: String?,
        endDate: String?,
        teamId: Long? = null,
        status: String? = null
    ): List<Match> {
        val matchesResponse = api.getMatchesWithDetails(
            matchTimeGte = startDate?.let { "gte.$it" },
            matchTimeLte = endDate?.let { "lte.$it" },
            status = status
        ).filter { dto ->
            teamId == null || dto.homeTeamId == teamId || dto.awayTeamId == teamId
        }

        val predictionsByMatchId = fetchPredictionsByMatchId(matchesResponse.map { it.id }.toSet())

        return matchesResponse.map { dto ->
            dto.toDomain().copy(prediction = predictionsByMatchId[dto.id])
        }
    }

    private suspend fun fetchSingleMatchLegacy(matchId: Long): Match? {
        val matchResponse = api.getMatchById(id = "eq.$matchId")
        val matchDto = matchResponse.firstOrNull() ?: return null

        val homeTeam = api.getTeamById(id = "eq.${matchDto.homeTeamId}").firstOrNull() ?: return null
        val awayTeam = api.getTeamById(id = "eq.${matchDto.awayTeamId}").firstOrNull() ?: return null
        val league = api.getLeagueById(id = "eq.${matchDto.leagueId}").firstOrNull() ?: return null
        val predictionsByMatchId = fetchPredictionsByMatchId(setOf(matchId))

        return Match(
            id = matchDto.id,
            homeTeam = homeTeam.toDomain(),
            awayTeam = awayTeam.toDomain(),
            league = league.toDomain(),
            matchTime = parseDateTime(matchDto.matchTime),
            status = MatchStatus.fromString(matchDto.status),
            score = if (matchDto.homeScore != null && matchDto.awayScore != null) {
                Score(matchDto.homeScore, matchDto.awayScore)
            } else {
                null
            },
            prediction = predictionsByMatchId[matchId],
            round = matchDto.round,
            roundNumber = matchDto.roundNumber
        )
    }

    private suspend fun fetchPredictionsByMatchId(matchIds: Set<Long>): Map<Long, Prediction> {
        if (matchIds.isEmpty()) return emptyMap()

        val matchIdFilter = "in.(${matchIds.joinToString(",")})"
        val predictions = api.getMatchPredictions(matchId = matchIdFilter)

        if (predictions.isEmpty()) return emptyMap()

        val explanations = try {
            api.getPredictionExplanations(matchId = matchIdFilter).associateBy { it.matchId }
        } catch (e: Exception) {
            AppLogger.w(TAG, "获取预测说明失败，继续使用概率数据", e)
            emptyMap()
        }

        return predictions.associate { predictionDto: PredictionDto ->
            val enrichedPrediction = explanations[predictionDto.matchId]?.let { explanation ->
                predictionDto.copy(predictionExplanations = listOf(explanation))
            } ?: predictionDto

            predictionDto.matchId to enrichedPrediction.toDomain()
        }
    }

    private suspend fun cacheMatches(matches: List<Match>) {
        if (matches.isEmpty()) return

        val teamIds = matches.flatMap { listOf(it.homeTeam.id, it.awayTeam.id) }.distinct()
        val existingTeams = teamDao.getTeamsByIds(teamIds).associateBy { it.id }
        val existingLeagues = leagueDao.getAllLeagues().associateBy { it.id }

        val teamsToCache = matches
            .flatMap { listOf(it.homeTeam, it.awayTeam) }
            .distinctBy { it.id }
            .map { team -> mergeTeam(team, existingTeams[team.id]).toEntity() }

        val leaguesToCache = matches
            .map { it.league }
            .distinctBy { it.id }
            .map { league -> mergeLeague(league, existingLeagues[league.id]).toEntity() }

        teamDao.insertTeams(teamsToCache)
        leagueDao.insertLeagues(leaguesToCache)
        matchDao.insertMatches(matches.map { it.toEntity() })

        val matchIds = matches.map { it.id }
        predictionDao.deletePredictionsByMatchIds(matchIds)

        val predictionsToCache = matches.mapNotNull { match ->
            match.prediction?.toEntity(match.id)
        }

        if (predictionsToCache.isNotEmpty()) {
            predictionDao.insertPredictions(predictionsToCache)
        }
    }

    private suspend fun getCachedMatches(
        startDate: String,
        endDate: String,
        teamId: Long? = null
    ): List<Match> {
        val cached = matchDao.getMatches(startDate, endDate).first().map { it.toDomain() }
        return if (teamId == null) {
            cached
        } else {
            cached.filter { it.homeTeam.id == teamId || it.awayTeam.id == teamId }
        }
    }

    private fun mergeTeam(team: com.aura.football.domain.model.Team, existing: TeamEntity?): com.aura.football.domain.model.Team {
        return if (existing == null) {
            team
        } else {
            team.copy(
                shortName = team.shortName ?: existing.shortName,
                nameZh = team.nameZh ?: existing.nameZh,
                shortNameZh = team.shortNameZh ?: existing.shortNameZh,
                logoUrl = team.logoUrl ?: existing.logoUrl
            )
        }
    }

    private fun mergeLeague(
        league: com.aura.football.domain.model.League,
        existing: LeagueEntity?
    ): com.aura.football.domain.model.League {
        return if (existing == null) {
            league
        } else {
            league.copy(
                country = league.country ?: existing.country,
                emblemUrl = league.emblemUrl ?: existing.emblemUrl
            )
        }
    }

    companion object {
        private const val TAG = "MatchRepository"
    }
}
