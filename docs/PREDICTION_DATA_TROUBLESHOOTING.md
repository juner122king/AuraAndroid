# 预测数据显示问题排查与解决

## 问题分析

### 当前状态 ✅
1. ✅ 本地数据库结构完整（`predictions` 表存在）
2. ✅ DAO 查询包含预测数据（`LEFT JOIN predictions`）
3. ✅ Entity Mapper 正确转换（`prediction?.toDomain()`）
4. ✅ UI 支持显示预测数据（Match 模型包含 prediction 字段）

### 问题根源 ❌
1. ❌ **后端视图未部署**：`match_predictions_view` 和 `get_match_predictions` RPC 还不存在
2. ❌ **未查询预测数据**：之前的代码没有单独获取预测数据的逻辑
3. ❌ **数据库为空**：本地数据库的 `predictions` 表没有数据

## 已实施的修复

### 1. 添加预测数据 API ✅
**文件**: `SupabaseApi.kt`
```kotlin
@GET("match_predictions")
suspend fun getMatchPredictions(
    @Query("select") select: String = "*, prediction_explanations(*)",
    @Query("match_id") matchId: String? = null
): List<PredictionDto>
```

### 2. 在 Repository 中获取预测数据 ✅
**文件**: `MatchRepositoryImpl.kt`

在 `getMatches()` 方法中添加了预测数据获取逻辑：
```kotlin
// 获取预测数据（临时方案：单独查询）
try {
    val predictions = api.getMatchPredictions()
    val matchIds = matchesResponse.map { it.id }.toSet()
    val relevantPredictions = predictions.filter { it.matchId in matchIds }

    if (relevantPredictions.isNotEmpty()) {
        relevantPredictions.forEach { predDto ->
            predictionDao.insertPrediction(predDto.toEntity())
        }
        Log.d(TAG, "成功获取并缓存 ${relevantPredictions.size} 条预测数据")
    }
} catch (predError: Exception) {
    Log.e(TAG, "获取预测数据失败: ${predError.message}", predError)
    // 预测数据失败不影响主流程
}
```

## 数据流程

### 当前实现（临时方案）
```
1. 获取比赛数据 (matches + teams + leagues)
   ↓
2. 单独获取预测数据 (match_predictions + prediction_explanations)
   ↓
3. 过滤相关的预测数据
   ↓
4. 缓存到本地数据库
   ↓
5. DAO 查询时通过 LEFT JOIN 关联
   ↓
6. Mapper 转换为 Domain 对象（包含 prediction）
   ↓
7. UI 显示
```

### 未来实现（后端视图部署后）
```
1. 调用 match_predictions_view 或 RPC
   ↓
2. 一次性获取所有数据（包含预测）
   ↓
3. 直接转换为 Domain 对象
   ↓
4. UI 显示
```

## 验证步骤

### 1. 检查后端数据
确认 Supabase 中是否有 `match_predictions` 表和数据：
```sql
SELECT * FROM match_predictions LIMIT 10;
```

### 2. 检查日志
运行应用后，查看 Logcat 中的日志：
```
MatchRepository: 成功获取并缓存 X 条预测数据
```

如果看到：
- ✅ "成功获取并缓存 N 条预测数据" → 有预测数据
- ⚠️ "没有预测数据" → 后端数据库无预测记录
- ❌ "获取预测数据失败" → API 调用失败（可能表不存在）

### 3. 检查本地数据库
使用 Database Inspector（Android Studio）查看：
```
App Inspection > Database Inspector > predictions 表
```

### 4. 检查 UI 显示
在比赛卡片上应该能看到：
- 预测概率（主胜 / 平局 / 客胜）
- 置信度
- 预测说明

## 可能的错误情况

### 错误 1: "match_predictions" 表不存在
```
Response: {"code":"42P01","details":null,"hint":null,"message":"relation \"match_predictions\" does not exist"}
```

**解决方案**: 需要在 Supabase 后端创建 `match_predictions` 表

### 错误 2: 无预测数据
日志显示："没有预测数据"

**原因**: 数据库中没有预测记录

**解决方案**:
1. 确认后端是否有预测生成逻辑
2. 手动插入测试数据验证流程

### 错误 3: API 权限问题
```
Response: {"code":"42501","message":"permission denied for table match_predictions"}
```

**解决方案**: 在 Supabase 中配置 RLS 策略：
```sql
-- 允许匿名用户读取预测数据
CREATE POLICY "Allow anonymous read access"
ON match_predictions
FOR SELECT
TO anon
USING (true);
```

## 临时测试数据

如果后端没有预测数据，可以手动插入测试数据：

```sql
-- 插入测试预测数据
INSERT INTO match_predictions (match_id, model_version, home_win_prob, draw_prob, away_win_prob, confidence)
VALUES
  (1, 'v1.0', 0.45, 0.30, 0.25, 0.75),
  (2, 'v1.0', 0.60, 0.25, 0.15, 0.82),
  (3, 'v1.0', 0.35, 0.35, 0.30, 0.65);

-- 插入预测说明
INSERT INTO prediction_explanations (prediction_id, explanation_text)
SELECT p.id, '基于历史数据和当前形势的AI预测'
FROM match_predictions p
WHERE p.model_version = 'v1.0';
```

## 下一步

### 短期（当前）
- [x] 添加单独的预测数据查询
- [x] 实现预测数据缓存
- [ ] 验证预测数据显示
- [ ] 检查后端表结构

### 长期（后端视图部署后）
- [ ] 执行 `supabase db push` 部署视图
- [ ] 切换到使用 `match_predictions_view`
- [ ] 删除临时的单独查询逻辑
- [ ] 性能测试和优化

## 当前代码状态

### 历史对局优化 ✅
- ✅ 服务端 OR 查询过滤
- ✅ 联赛优先策略
- ✅ 结果数量限制
- ✅ 嵌入查询

### 预测数据集成 ⚠️
- ✅ API 方法已添加
- ✅ Repository 逻辑已实现
- ✅ 数据流程完整
- ⚠️ **等待后端数据确认**

### 后端视图（未部署） 🔜
- 🔜 `match_predictions_view` 视图
- 🔜 `get_match_predictions` RPC
- 🔜 前端切换到视图查询
