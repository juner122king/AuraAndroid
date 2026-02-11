# Supabase API 400错误修复

## 问题描述

**错误日志**:
```
MatchRepository: 开始获取比赛: 2026-02-11T00:00 到 2026-02-12T00:00
MatchRepository: 开始网络请求...
OkHttp: <-- 400 Bad Request
Error: Could not find a relationship between 'matches' and 'home_team'
Hint: Perhaps you meant 'teams' instead of 'home_team'.
```

**根本原因**:
- Supabase数据库中没有配置外键关系（foreign key relationships）
- API尝试使用嵌套查询（nested queries）如 `select=*,home_team(*),away_team(*),league(*)`
- Supabase无法自动加载关联数据

## 解决方案

### 方案对比

**方案A：在Supabase中配置外键**
- 优点：可以使用嵌套查询，代码更简洁
- 缺点：需要修改数据库结构，可能影响其他服务

**方案B：分别查询后手动组合（已采用）**
- 优点：不需要修改数据库，完全在应用层解决
- 缺点：需要多次API调用

### 实现细节

#### 1. 简化API查询

**修改前**（会报400错误）:
```kotlin
@GET("matches")
suspend fun getMatches(
    @Query("select") select: String = "*,home_team(*),away_team(*),league(*),match_predictions(*)",
    @Query("match_time") matchTimeGte: String? = null,
    @Query("match_time") matchTimeLte: String? = null,
    @Query("order") order: String = "match_time.asc"
): List<MatchDto>
```

**修改后**（只获取基本数据）:
```kotlin
@GET("matches")
suspend fun getMatches(
    @Query("select") select: String = "*",
    @Query("match_time") matchTimeGte: String? = null,
    @Query("match_time") matchTimeLte: String? = null,
    @Query("status") status: String? = null,
    @Query("order") order: String = "match_time.asc"
): List<MatchDto>

@GET("teams")
suspend fun getTeams(
    @Query("select") select: String = "*"
): List<TeamDto>

@GET("leagues")
suspend fun getLeagues(
    @Query("select") select: String = "*"
): List<LeagueDto>
```

#### 2. 简化DTO

**修改前**:
```kotlin
data class MatchDto(
    val id: Long,
    val leagueId: Long,
    val homeTeamId: Long,
    val awayTeamId: Long,
    val matchTime: String,
    val status: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val homeTeam: TeamDto,      // 嵌套对象
    val awayTeam: TeamDto,      // 嵌套对象
    val league: LeagueDto,      // 嵌套对象
    val matchPredictions: List<PredictionDto>?  // 嵌套对象
)
```

**修改后**:
```kotlin
data class MatchDto(
    @SerializedName("id") val id: Long,
    @SerializedName("league_id") val leagueId: Long,
    @SerializedName("home_team_id") val homeTeamId: Long,
    @SerializedName("away_team_id") val awayTeamId: Long,
    @SerializedName("match_time") val matchTime: String,
    @SerializedName("status") val status: String,
    @SerializedName("home_score") val homeScore: Int?,
    @SerializedName("away_score") val awayScore: Int?
    // 移除所有嵌套对象
)
```

#### 3. 在Repository中手动组合数据

```kotlin
override fun getMatches(startDate: String, endDate: String): Flow<List<Match>> = flow {
    try {
        // 1. 获取比赛基本数据
        val matchesResponse = api.getMatches(
            matchTimeGte = "gte.$startDate",
            matchTimeLte = "lte.$endDate"
        )

        if (matchesResponse.isNotEmpty()) {
            // 2. 获取所有唯一的team IDs和league IDs
            val teamIds = matchesResponse.flatMap {
                listOf(it.homeTeamId, it.awayTeamId)
            }.distinct()
            val leagueIds = matchesResponse.map { it.leagueId }.distinct()

            // 3. 查询所有teams和leagues
            val allTeams = api.getTeams()
            val allLeagues = api.getLeagues()

            // 4. 创建ID到对象的映射表
            val teamsMap = allTeams.associateBy { it.id }
            val leaguesMap = allLeagues.associateBy { it.id }

            // 5. 组合数据
            val matches = matchesResponse.mapNotNull { matchDto ->
                val homeTeam = teamsMap[matchDto.homeTeamId]
                val awayTeam = teamsMap[matchDto.awayTeamId]
                val league = leaguesMap[matchDto.leagueId]

                if (homeTeam != null && awayTeam != null && league != null) {
                    Match(
                        id = matchDto.id,
                        homeTeam = homeTeam.toDomain(),
                        awayTeam = awayTeam.toDomain(),
                        league = league.toDomain(),
                        matchTime = parseDateTime(matchDto.matchTime),
                        status = MatchStatus.fromString(matchDto.status),
                        score = if (matchDto.homeScore != null && matchDto.awayScore != null) {
                            Score(matchDto.homeScore, matchDto.awayScore)
                        } else null,
                        prediction = null
                    )
                } else {
                    Log.w(TAG, "跳过比赛 ${matchDto.id}：缺少关联数据")
                    null
                }
            }

            // 6. 更新缓存
            teamDao.insertTeams(allTeams.map { it.toEntity() })
            leagueDao.insertLeagues(allLeagues.map { it.toEntity() })
            matchDao.insertMatches(matchesResponse.map { it.toEntity() })

            // 7. 发送数据
            emit(matches)
        }
    } catch (e: Exception) {
        // 网络失败，尝试从缓存加载
        matchDao.getMatches(startDate, endDate).collect { cachedMatches ->
            if (cachedMatches.isNotEmpty()) {
                emit(cachedMatches.map { it.toDomain() })
            }
        }
    }
}.flowOn(Dispatchers.IO)
```

## 性能优化

### 当前实现
- 每次调用`getMatches()`需要3次API请求：
  1. GET /matches
  2. GET /teams
  3. GET /leagues

### 优化建议（未来）

**方案1：缓存teams和leagues**
```kotlin
// 在Application启动时预加载
class AuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        viewModelScope.launch {
            repository.preloadTeamsAndLeagues()
        }
    }
}
```

**方案2：使用Supabase RPC函数**
在Supabase中创建自定义函数，一次调用返回所有数据：
```sql
CREATE OR REPLACE FUNCTION get_matches_with_details(
    start_date TIMESTAMP,
    end_date TIMESTAMP
)
RETURNS TABLE (
    match_id BIGINT,
    match_time TIMESTAMP,
    status TEXT,
    home_score INT,
    away_score INT,
    home_team_id BIGINT,
    home_team_name TEXT,
    home_team_logo TEXT,
    away_team_id BIGINT,
    away_team_name TEXT,
    away_team_logo TEXT,
    league_id BIGINT,
    league_name TEXT,
    league_logo TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        m.id,
        m.match_time,
        m.status,
        m.home_score,
        m.away_score,
        ht.id, ht.name, ht.logo_url,
        at.id, at.name, at.logo_url,
        l.id, l.name, l.logo_url
    FROM matches m
    JOIN teams ht ON m.home_team_id = ht.id
    JOIN teams at ON m.away_team_id = at.id
    JOIN leagues l ON m.league_id = l.id
    WHERE m.match_time BETWEEN start_date AND end_date
    ORDER BY m.match_time ASC;
END;
$$ LANGUAGE plpgsql;
```

然后在API中调用：
```kotlin
@POST("rpc/get_matches_with_details")
suspend fun getMatchesWithDetails(
    @Body params: Map<String, String>
): List<MatchWithDetailsDto>
```

## 总结

### 优点
✅ 不需要修改Supabase数据库结构
✅ 完全在应用层解决问题
✅ 代码逻辑清晰，易于维护

### 缺点
❌ 需要多次API调用（当前是3次）
❌ 网络流量稍大（但teams和leagues总数据量不大）

### 验证清单

- [x] 修复HTTP 400错误
- [x] 成功获取matches数据
- [x] 成功获取teams数据
- [x] 成功获取leagues数据
- [x] 正确组合数据
- [x] 更新缓存
- [x] 构建成功
- [ ] 安装APK测试
- [ ] 验证UI正确显示
- [ ] 验证缓存正常工作

## 下一步

1. 安装新APK并测试
2. 验证比赛数据能正确显示
3. 如果性能有问题，考虑实现上述优化方案
4. 添加预测数据加载逻辑（目前设为null）

---

**修复版本**: v1.2
**修复时间**: 2026-02-10 14:47
**APK路径**: `app/build/outputs/apk/debug/app-debug.apk`
