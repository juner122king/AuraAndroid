package com.aura.football.data.remote

import com.aura.football.data.remote.dto.ExplanationDto
import com.aura.football.data.remote.dto.LeagueDto
import com.aura.football.data.remote.dto.MatchDto
import com.aura.football.data.remote.dto.MatchPredictionViewDto
import com.aura.football.data.remote.dto.MatchPredictionsRpcParams
import com.aura.football.data.remote.dto.MatchWithDetailsDto
import com.aura.football.data.remote.dto.PredictionDto
import com.aura.football.data.remote.dto.StandingDto
import com.aura.football.data.remote.dto.TeamDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApi {

    /**
     * 获取比赛数据（包含嵌套的team和league信息）
     * 使用PostgREST的嵌入查询一次性获取所有关联数据
     */
    @GET("matches")
    suspend fun getMatchesWithDetails(
        @Query("select") select: String = "*, home_team:teams!matches_home_team_id_fkey(*), away_team:teams!matches_away_team_id_fkey(*), league:leagues(*)",
        @Query("match_time") matchTimeGte: String? = null,
        @Query("match_time") matchTimeLte: String? = null,
        @Query("status") status: String? = null,
        @Query("league_id") leagueId: Long? = null,
        @Query("order") order: String = "match_time.asc"
    ): List<MatchWithDetailsDto>

    // 简化查询：只获取matches基本数据
    @GET("matches")
    suspend fun getMatches(
        @Query("select") select: String = "*",
        @Query("match_time") matchTimeGte: String? = null,
        @Query("match_time") matchTimeLte: String? = null,
        @Query("status") status: String? = null,
        @Query("league_id") leagueId: Long? = null,
        @Query("order") order: String = "match_time.asc"
    ): List<MatchDto>

    @GET("matches")
    suspend fun getMatchById(
        @Query("select") select: String = "*",
        @Query("id") id: String
    ): List<MatchDto>

    // 单独查询teams
    @GET("teams")
    suspend fun getTeams(
        @Query("select") select: String = "*"
    ): List<TeamDto>

    @GET("teams")
    suspend fun getTeamById(
        @Query("select") select: String = "*",
        @Query("id") id: String
    ): List<TeamDto>

    // 单独查询leagues
    @GET("leagues")
    suspend fun getLeagues(
        @Query("select") select: String = "*"
    ): List<LeagueDto>

    @GET("leagues")
    suspend fun getLeagueById(
        @Query("select") select: String = "*",
        @Query("id") id: String
    ): List<LeagueDto>

    @GET("league_standings")
    suspend fun getStandings(
        @Query("select") select: String = "*",
        @Query("league_id") leagueId: String,
        @Query("order") order: String = "position.asc"
    ): List<StandingDto>

    // 单独查询match_predictions（临时方案，用于后端视图部署前）
    @GET("match_predictions")
    suspend fun getMatchPredictions(
        @Query("select") select: String = "*",  // 暂时不嵌套查询
        @Query("match_id") matchId: String? = null
    ): List<PredictionDto>

    // 单独查询prediction_explanations
    @GET("prediction_explanations")
    suspend fun getPredictionExplanations(
        @Query("select") select: String = "*",
        @Query("match_id") matchId: String? = null
    ): List<ExplanationDto>

    /**
     * 获取历史对局数据（优化版）
     * 使用服务端过滤和嵌入查询一次性获取完整数据
     * @param orCondition 两队对局的OR查询条件
     * @param leagueId 可选的联赛ID过滤
     * @param limit 返回数量限制
     */
    @GET("matches")
    suspend fun getHistoricalMatchups(
        @Query("select") select: String = "*, home_team:teams!matches_home_team_id_fkey(*), away_team:teams!matches_away_team_id_fkey(*), league:leagues(*)",
        @Query("status") status: String = "eq.finished",
        @Query("or") orCondition: String,
        @Query("league_id") leagueId: String? = null,
        @Query("order") order: String = "match_time.desc",
        @Query("limit") limit: Int? = null
    ): List<MatchWithDetailsDto>

    // ========== 新增：match_predictions_view 视图查询 ==========

    /**
     * 从 match_predictions_view 视图获取比赛和预测数据（方式1：直接查询视图）
     * 聚合了 matches + teams + leagues + match_predictions + prediction_explanations
     */
    @GET("match_predictions_view")
    suspend fun getMatchPredictionsFromView(
        @Query("select") select: String = "*",
        @Query("match_time") matchTimeGte: String? = null,
        @Query("match_time") matchTimeLte: String? = null,
        @Query("status") status: String? = null,
        @Query("league_id") leagueId: String? = null,
        @Query("order") order: String = "match_time.asc",
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): List<MatchPredictionViewDto>

    /**
     * 调用 get_match_predictions RPC 函数（方式2：使用RPC，功能更强大）
     * 支持更灵活的过滤和分页
     */
    @POST("rpc/get_match_predictions")
    suspend fun getMatchPredictionsRpc(
        @Body params: MatchPredictionsRpcParams
    ): List<MatchPredictionViewDto>
}
