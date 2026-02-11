package com.aura.football.data.repository

import android.util.Log
import com.aura.football.data.local.dao.LeagueDao
import com.aura.football.data.local.dao.MatchDao
import com.aura.football.data.local.dao.PredictionDao
import com.aura.football.data.local.dao.TeamDao
import com.aura.football.data.local.entity.toDomain
import com.aura.football.data.local.entity.toEntity
import com.aura.football.data.remote.SupabaseApi
import com.aura.football.data.remote.dto.MatchPredictionsRpcParams
import com.aura.football.data.remote.dto.MatchWithDetailsDto
import com.aura.football.data.remote.dto.toDomain
import com.aura.football.domain.model.HistoricalMatch
import com.aura.football.domain.model.HistoricalMatchupStats
import com.aura.football.domain.model.Match
import com.aura.football.domain.repository.MatchRepository
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

    /**
     * 获取比赛列表（优化版：使用嵌入式查询）
     */
    override fun getMatches(startDate: String, endDate: String): Flow<List<Match>> = flow {
        Log.d(TAG, "开始获取比赛: $startDate 到 $endDate")

        var emittedCache = false

        // 优先从网络获取（使用嵌入式查询，一次性获取所有数据）
        try {
            Log.d(TAG, "开始网络请求（嵌入式查询）...")
            val matchesResponse = api.getMatchesWithDetails(
                matchTimeGte = "gte.$startDate",
                matchTimeLte = "lte.$endDate"
            )

            Log.d(TAG, "网络请求成功，收到 ${matchesResponse.size} 场完整比赛数据")

            if (matchesResponse.isNotEmpty()) {
                // 直接转换为Domain对象（数据已完整）
                val matches = matchesResponse.map { it.toDomain() }

                // 缓存到本地数据库
                val teams = matchesResponse.flatMap {
                    listOf(it.homeTeam, it.awayTeam)
                }.distinctBy { it.id }

                val leagues = matchesResponse.map { it.league }.distinctBy { it.id }

                teamDao.insertTeams(teams.map { it.toEntity() })
                leagueDao.insertLeagues(leagues.map { it.toEntity() })
                matchDao.insertMatches(matchesResponse.map { dto ->
                    com.aura.football.data.remote.dto.MatchDto(
                        id = dto.id,
                        homeTeamId = dto.homeTeamId,
                        awayTeamId = dto.awayTeamId,
                        leagueId = dto.leagueId,
                        matchTime = dto.matchTime,
                        status = dto.status,
                        homeScore = dto.homeScore,
                        awayScore = dto.awayScore
                    ).toEntity()
                })

                // 获取预测数据（临时方案：分开查询，然后合并）
                try {
                    Log.d(TAG, "开始获取预测数据...")

                    // 1. 获取预测概率
                    val predictions = api.getMatchPredictions()
                    val matchIds = matchesResponse.map { it.id }.toSet()
                    val relevantPredictions = predictions.filter { it.matchId in matchIds }

                    if (relevantPredictions.isNotEmpty()) {
                        Log.d(TAG, "找到 ${relevantPredictions.size} 条预测数据")

                        // 2. 获取预测说明
                        try {
                            val explanations = api.getPredictionExplanations()
                            val explanationsMap = explanations.associateBy { it.matchId }

                            // 3. 合并预测和说明
                            relevantPredictions.forEach { predDto ->
                                // 将说明添加到 predictionExplanations 列表中
                                val explanation = explanationsMap[predDto.matchId]
                                val predWithExplanation = if (explanation != null) {
                                    predDto.copy(predictionExplanations = listOf(explanation))
                                } else {
                                    predDto
                                }

                                predictionDao.insertPrediction(predWithExplanation.toEntity())
                            }

                            Log.d(TAG, "成功获取并缓存 ${relevantPredictions.size} 条预测数据（含 ${explanations.filter { it.matchId in matchIds }.size} 条说明）")
                        } catch (expError: Exception) {
                            Log.e(TAG, "获取预测说明失败: ${expError.message}", expError)
                            // 即使说明失败，也保存预测概率
                            relevantPredictions.forEach { predDto ->
                                predictionDao.insertPrediction(predDto.toEntity())
                            }
                            Log.d(TAG, "成功获取并缓存 ${relevantPredictions.size} 条预测数据（无说明）")
                        }
                    } else {
                        Log.d(TAG, "没有预测数据")
                    }
                } catch (predError: Exception) {
                    Log.e(TAG, "获取预测数据失败: ${predError.message}", predError)
                    // 预测数据失败不影响主流程
                }

                Log.d(TAG, "成功获取并缓存 ${matches.size} 场比赛数据")

                // 从数据库重新查询以获取完整数据（包含刚插入的prediction）
                val freshData = matchDao.getMatches(startDate, endDate).first()
                val freshMatches = freshData.map { it.toDomain() }
                val predictionsCount = freshMatches.count { it.prediction != null }

                Log.d(TAG, "从数据库加载完整数据: ${freshMatches.size} 场比赛, $predictionsCount 场有预测")

                emit(freshMatches)
                emittedCache = true
            } else {
                emit(emptyList())
                emittedCache = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "网络请求失败: ${e.message}", e)

            // 网络失败，尝试从缓存加载
            try {
                matchDao.getMatches(startDate, endDate).collect { cachedMatches ->
                    if (cachedMatches.isNotEmpty()) {
                        Log.d(TAG, "网络失败，从缓存加载 ${cachedMatches.size} 场比赛")
                        emit(cachedMatches.map { it.toDomain() })
                        emittedCache = true
                    }
                }
            } catch (cacheError: Exception) {
                Log.e(TAG, "读取缓存也失败", cacheError)
            }

            // 如果没有emit任何数据，抛出原始错误
            if (!emittedCache) {
                throw e
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 将UTC时间字符串转换为本地时间
     * 支持多种ISO格式：
     * - 2026-02-06T19:00:00Z
     * - 2026-02-06T19:00:00+00:00
     * - 2026-02-06T19:00:00
     */
    private fun parseDateTime(dateString: String): java.time.LocalDateTime {
        return try {
            val instant = when {
                // 格式1: 2026-02-06T19:00:00Z
                dateString.endsWith("Z") -> {
                    java.time.Instant.parse(dateString)
                }
                // 格式2: 2026-02-06T19:00:00+00:00 或其他时区偏移
                dateString.contains("+") || dateString.lastIndexOf("-") > 10 -> {
                    // 使用OffsetDateTime解析带时区偏移的时间
                    java.time.OffsetDateTime.parse(dateString).toInstant()
                }
                // 格式3: 2026-02-06T19:00:00 (假设是UTC)
                else -> {
                    java.time.Instant.parse("${dateString}Z")
                }
            }

            // 转换为系统默认时区的本地时间
            instant.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        } catch (e: Exception) {
            try {
                // 如果上面失败，尝试直接解析为LocalDateTime（假设已经是本地时间）
                java.time.LocalDateTime.parse(dateString, java.time.format.DateTimeFormatter.ISO_DATE_TIME)
            } catch (e: Exception) {
                // 如果都失败，返回当前时间
                java.time.LocalDateTime.now()
            }
        }
    }

    companion object {
        private const val TAG = "MatchRepository"
    }

    override suspend fun getMatchById(matchId: Long): Match? {
        return try {
            // Try cache first
            matchDao.getMatchById(matchId)?.toDomain() ?: run {
                // Fetch from network
                val matchResponse = api.getMatchById(id = "eq.$matchId")
                matchResponse.firstOrNull()?.let { matchDto ->
                    // 获取关联的teams和league
                    val homeTeamResponse = api.getTeamById(id = "eq.${matchDto.homeTeamId}")
                    val awayTeamResponse = api.getTeamById(id = "eq.${matchDto.awayTeamId}")
                    val leagueResponse = api.getLeagueById(id = "eq.${matchDto.leagueId}")

                    val homeTeam = homeTeamResponse.firstOrNull()
                    val awayTeam = awayTeamResponse.firstOrNull()
                    val league = leagueResponse.firstOrNull()

                    if (homeTeam != null && awayTeam != null && league != null) {
                        // Update cache
                        teamDao.insertTeam(homeTeam.toEntity())
                        teamDao.insertTeam(awayTeam.toEntity())
                        leagueDao.insertLeague(league.toEntity())
                        matchDao.insertMatch(matchDto.toEntity())

                        // 组合成Match对象
                        Match(
                            id = matchDto.id,
                            homeTeam = homeTeam.toDomain(),
                            awayTeam = awayTeam.toDomain(),
                            league = league.toDomain(),
                            matchTime = parseDateTime(matchDto.matchTime),
                            status = com.aura.football.domain.model.MatchStatus.fromString(matchDto.status),
                            score = if (matchDto.homeScore != null && matchDto.awayScore != null) {
                                com.aura.football.domain.model.Score(matchDto.homeScore, matchDto.awayScore)
                            } else null,
                            prediction = null
                        )
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun updateLiveMatches() {
        try {
            val liveMatches = api.getMatches(status = "eq.live")

            if (liveMatches.isNotEmpty()) {
                // 获取所有唯一的team IDs和league IDs
                val teamIds = liveMatches.flatMap { listOf(it.homeTeamId, it.awayTeamId) }.distinct()
                val leagueIds = liveMatches.map { it.leagueId }.distinct()

                // 查询所有teams和leagues
                val allTeams = api.getTeams()
                val allLeagues = api.getLeagues()

                // 只插入需要的teams和leagues
                val teamsMap = allTeams.associateBy { it.id }
                val leaguesMap = allLeagues.associateBy { it.id }

                val neededTeams = teamIds.mapNotNull { teamsMap[it] }
                val neededLeagues = leagueIds.mapNotNull { leaguesMap[it] }

                teamDao.insertTeams(neededTeams.map { it.toEntity() })
                leagueDao.insertLeagues(neededLeagues.map { it.toEntity() })
                matchDao.insertMatches(liveMatches.map { it.toEntity() })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun getHistoricalMatchups(
        homeTeamId: Long,
        awayTeamId: Long,
        leagueId: Long?
    ): HistoricalMatchupStats {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "开始获取历史对局: $homeTeamId vs $awayTeamId (联赛ID: $leagueId)")

            try {
                // 构造 OR 查询条件：(home=A and away=B) or (home=B and away=A)
                val orCondition = "(and(home_team_id.eq.$homeTeamId,away_team_id.eq.$awayTeamId),and(home_team_id.eq.$awayTeamId,away_team_id.eq.$homeTeamId))"

                // 查询结果限制为最近20场对局
                val limit = 20

                var matchesResponse: List<MatchWithDetailsDto>

                // 如果知道联赛ID，先查询该联赛的历史对局
                if (leagueId != null) {
                    Log.d(TAG, "优先查询联赛 $leagueId 的历史对局")
                    val leagueMatches = api.getHistoricalMatchups(
                        orCondition = orCondition,
                        leagueId = "eq.$leagueId",
                        limit = limit
                    )

                    Log.d(TAG, "该联赛中找到 ${leagueMatches.size} 场历史对局")

                    // 如果该联赛有足够的对局记录（≥3场），直接使用
                    if (leagueMatches.size >= 3) {
                        matchesResponse = leagueMatches
                    } else {
                        // 联赛内记录不足，查询所有联赛的对局
                        Log.d(TAG, "联赛内对局记录不足，扩展查询所有联赛")
                        matchesResponse = api.getHistoricalMatchups(
                            orCondition = orCondition,
                            limit = limit
                        )
                    }
                } else {
                    // 没有联赛ID，直接查询所有联赛
                    Log.d(TAG, "查询所有联赛的历史对局")
                    matchesResponse = api.getHistoricalMatchups(
                        orCondition = orCondition,
                        limit = limit
                    )
                }

                Log.d(TAG, "总共找到 ${matchesResponse.size} 场历史对局")

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

                // 直接使用嵌入查询返回的完整数据构建 HistoricalMatch 列表
                // 数据已经按 match_time desc 排序
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
                            status = com.aura.football.domain.model.MatchStatus.fromString(dto.status)
                        )
                    } else null
                }

                // 计算统计数据（从当前主队视角）
                var homeWins = 0
                var awayWins = 0
                var draws = 0
                var homeGoals = 0
                var awayGoals = 0

                historicalMatches.forEach { match ->
                    // 判断当前主队在历史比赛中是主场还是客场
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

                Log.d(TAG, "历史对局统计: ${homeWins}胜 ${draws}平 ${awayWins}负, 进球 $homeGoals:$awayGoals")

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
                Log.e(TAG, "获取历史对局失败: ${e.message}", e)
                // 返回空数据而不是抛出异常
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
}
