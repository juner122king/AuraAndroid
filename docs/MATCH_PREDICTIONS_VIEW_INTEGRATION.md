# Match Predictions View 集成文档

## 概述

后端已提供聚合视图 `match_predictions_view` 和 RPC 函数 `get_match_predictions`，可以一次性获取比赛、球队、联赛和预测数据，避免多次查询。

## 后端资源

### 1. 视图: `match_predictions_view`
聚合了以下数据：
- matches (比赛基本信息)
- teams (主队和客队)
- leagues (联赛信息)
- match_predictions (预测概率)
- prediction_explanations (预测说明)

### 2. RPC: `get_match_predictions`
提供更强大的过滤和分页功能。

## 前端集成

### 已完成的修改

#### 1. DTO 层 ✅

**新增文件**: `MatchPredictionViewDto.kt`
```kotlin
data class MatchPredictionViewDto(
    // Match fields
    val matchId: Long,
    val matchTime: String,
    val status: String,
    val homeScore: Int?,
    val awayScore: Int?,

    // Team fields
    val homeTeamId: Long,
    val homeTeamName: String,
    val homeTeamLogoUrl: String?,
    val awayTeamId: Long,
    val awayTeamName: String,
    val awayTeamLogoUrl: String?,

    // League fields
    val leagueId: Long,
    val leagueName: String,
    val leagueLogoUrl: String?,
    val country: String?,

    // Prediction fields (nullable)
    val predictionId: Long?,
    val modelVersion: String?,
    val homeWinProb: Float?,
    val drawProb: Float?,
    val awayWinProb: Float?,
    val confidence: Float?,
    val explanations: List<String>?
)
```

**新增参数类**: `MatchPredictionsRpcParams`
```kotlin
data class MatchPredictionsRpcParams(
    val fromTs: String,
    val toTs: String,
    val leagueIdFilter: Long? = null,
    val statusFilter: String? = null,
    val onlyPredicted: Boolean? = false,
    val limitCount: Int? = 50,
    val offsetCount: Int? = 0
)
```

#### 2. API 层 ✅

**文件**: `SupabaseApi.kt`

**方式1: 直接查询视图**（推荐，性能更好）
```kotlin
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
```

**方式2: 使用 RPC**（功能更强大）
```kotlin
@POST("rpc/get_match_predictions")
suspend fun getMatchPredictionsRpc(
    @Body params: MatchPredictionsRpcParams
): List<MatchPredictionViewDto>
```

#### 3. Mapper 层 ✅

**文件**: `Mappers.kt`

新增转换函数：
```kotlin
fun MatchPredictionViewDto.toDomain(): Match {
    // 将视图数据转换为 Match 对象（包含预测）
    // 自动组装 homeTeam, awayTeam, league, prediction
}
```

#### 4. Repository 层 ✅

**文件**: `MatchRepositoryImpl.kt`

已更新 `getMatches()` 方法使用新的视图：

```kotlin
override fun getMatches(startDate: String, endDate: String): Flow<List<Match>> = flow {
    // 方式1: 使用视图（推荐）
    val matchesResponse = api.getMatchPredictionsFromView(
        matchTimeGte = "gte.$startDate",
        matchTimeLte = "lte.$endDate",
        order = "match_time.asc"
    )

    // 方式2: 使用 RPC（功能更强大）
    // val matchesResponse = api.getMatchPredictionsRpc(
    //     MatchPredictionsRpcParams(
    //         fromTs = startDate,
    //         toTs = endDate,
    //         onlyPredicted = false,
    //         limitCount = 100
    //     )
    // )

    // 转换并发射数据
    emit(matchesResponse.map { it.toDomain() })
}
```

## 使用方式

### 方式1: 直接查询视图（推荐）

适用场景：简单的时间范围和状态过滤

```kotlin
// 在 Repository 中
val matches = api.getMatchPredictionsFromView(
    matchTimeGte = "gte.2026-02-11T00:00:00Z",
    matchTimeLte = "lte.2026-02-18T00:00:00Z",
    status = "eq.scheduled",
    order = "match_time.asc",
    limit = 50
)
```

**PostgREST 查询示例**:
```
GET /match_predictions_view?match_time=gte.2026-02-11T00:00:00Z&match_time=lte.2026-02-18T00:00:00Z&status=eq.scheduled&order=match_time.asc&limit=50
```

### 方式2: 使用 RPC（功能更强大）

适用场景：需要高级过滤（如只返回有预测的比赛）

```kotlin
// 在 Repository 中
val matches = api.getMatchPredictionsRpc(
    MatchPredictionsRpcParams(
        fromTs = "2026-02-11T00:00:00Z",
        toTs = "2026-02-18T00:00:00Z",
        leagueIdFilter = 1,  // 可选：过滤特定联赛
        statusFilter = "scheduled",  // 可选：过滤状态
        onlyPredicted = true,  // 只返回有预测的比赛
        limitCount = 50,
        offsetCount = 0
    )
)
```

**RPC 调用示例**:
```
POST /rpc/get_match_predictions
Content-Type: application/json

{
  "from_ts": "2026-02-11T00:00:00Z",
  "to_ts": "2026-02-18T00:00:00Z",
  "league_id_filter": 1,
  "status_filter": "scheduled",
  "only_predicted": true,
  "limit_count": 50,
  "offset_count": 0
}
```

## 性能优势

### 旧方案（多次查询）
1. 查询 matches
2. 查询 teams
3. 查询 leagues
4. 查询 match_predictions
5. 客户端拼接数据

**问题**:
- 4-5 次网络请求
- 大量冗余数据传输
- 客户端处理负担重

### 新方案（单次查询视图）
1. 一次查询 `match_predictions_view`
2. 后端已聚合所有数据
3. 直接转换为 Domain 对象

**优势**:
- ✅ 1 次网络请求（性能提升 4-5 倍）
- ✅ 精确数据，无冗余
- ✅ 后端聚合，客户端轻量
- ✅ 自动包含预测数据

## 数据字段说明

### 比赛字段
- `match_id`: 比赛ID
- `match_time`: 比赛时间
- `status`: 比赛状态 (scheduled, live, finished)
- `home_score`, `away_score`: 比分（可为 null）

### 球队字段
- `home_team_id`, `home_team_name`, `home_team_logo_url`
- `away_team_id`, `away_team_name`, `away_team_logo_url`

### 联赛字段
- `league_id`, `league_name`, `league_logo_url`, `country`

### 预测字段（如果有预测）
- `prediction_id`: 预测记录ID（为 null 表示无预测）
- `model_version`: 模型版本
- `home_win_prob`: 主队胜率 (0.0-1.0)
- `draw_prob`: 平局概率 (0.0-1.0)
- `away_win_prob`: 客队胜率 (0.0-1.0)
- `confidence`: 置信度 (0.0-1.0)
- `explanations`: 预测说明列表

## 切换使用新 API

### 当前状态
✅ `MatchRepositoryImpl.getMatches()` 已切换到 `match_predictions_view`

### 切换到 RPC（可选）
如果需要使用 RPC 的高级功能（如 `only_predicted`），取消注释 Repository 中的 RPC 调用代码：

```kotlin
// 注释掉视图调用
// val matchesResponse = api.getMatchPredictionsFromView(...)

// 启用 RPC 调用
val matchesResponse = api.getMatchPredictionsRpc(
    MatchPredictionsRpcParams(
        fromTs = startDate,
        toTs = endDate,
        onlyPredicted = false,  // 根据需求调整
        limitCount = 100
    )
)
```

## 测试建议

### 1. 功能测试
- [ ] 获取有预测的比赛，验证预测数据正确显示
- [ ] 获取无预测的比赛，验证 prediction 为 null
- [ ] 测试时间范围过滤
- [ ] 测试联赛过滤
- [ ] 测试分页功能

### 2. 性能测试
- [ ] 对比新旧方案的网络请求次数
- [ ] 对比数据传输量
- [ ] 测试大数据量场景（100+ 场比赛）

### 3. 边界测试
- [ ] 无数据情况
- [ ] 网络错误回退到缓存
- [ ] 部分数据缺失（如 logo_url 为 null）

## 注意事项

1. **时间格式**: 使用 ISO 8601 格式 (YYYY-MM-DDTHH:mm:ssZ)
2. **PostgREST 过滤**: 使用 `gte.`, `lte.`, `eq.` 等前缀
3. **预测数据**: 可能为 null，需做空值处理
4. **缓存策略**: 预测数据也会被缓存到本地数据库
5. **向后兼容**: 保留了旧的 `getMatchesWithDetails()` 方法作为备用

## 下一步优化

1. **缓存策略**: 添加预测数据的缓存过期时间
2. **增量更新**: 只更新变化的预测数据
3. **分页优化**: 实现虚拟滚动或分页加载
4. **WebSocket**: 考虑实时更新预测数据
