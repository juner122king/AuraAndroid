# 添加外键约束以支持 PostgREST 嵌套查询

## 问题
PostgREST 需要外键关系才能执行嵌套查询：
```
match_predictions?select=*,prediction_explanations(*)
```

但目前 `prediction_explanations` 表没有定义外键指向 `match_predictions`。

## 解决方案：添加外键约束

在 Supabase SQL Editor 中执行：

```sql
-- 1. 确认当前表结构
\d match_predictions
\d prediction_explanations

-- 2. 检查是否已有外键约束
SELECT
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_name IN ('match_predictions', 'prediction_explanations');

-- 3. 添加外键约束
-- 方式 1: 如果 match_predictions 的主键是 match_id
ALTER TABLE prediction_explanations
ADD CONSTRAINT fk_prediction_explanations_match_id
FOREIGN KEY (match_id)
REFERENCES match_predictions(match_id)
ON DELETE CASCADE;

-- 方式 2: 如果需要通过中间表 matches 关联
-- （根据实际表结构调整）

-- 4. 验证外键已创建
SELECT
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_name = 'prediction_explanations';

-- 5. 刷新 PostgREST schema cache（在 Supabase 中通常自动完成）
-- 如果使用自托管的 PostgREST，需要手动刷新：
-- NOTIFY pgrst, 'reload schema';
```

## 测试嵌套查询

添加外键后，测试 API：

```
GET /match_predictions?select=*,prediction_explanations(*)&limit=1
```

应该返回嵌套的数据结构：
```json
[
  {
    "match_id": 1122,
    "model_version": "rule-v1",
    "home_win_prob": 0.6545,
    "draw_prob": 0.2392,
    "away_win_prob": 0.1064,
    "confidence": 1,
    "generated_at": "2026-02-11T07:10:02.881+00:00",
    "prediction_explanations": [
      {
        "match_id": 1122,
        "explanation": "预测说明",
        "generated_at": "2026-02-11T07:10:02.881+00:00"
      }
    ]
  }
]
```

## 注意事项

1. **主键确认**: 确保 `match_predictions` 表的主键是 `match_id`
   ```sql
   SELECT constraint_name, constraint_type
   FROM information_schema.table_constraints
   WHERE table_name = 'match_predictions'
     AND constraint_type = 'PRIMARY KEY';
   ```

2. **数据一致性**: 添加外键前，确保 `prediction_explanations` 中的所有 `match_id` 都存在于 `match_predictions` 中
   ```sql
   -- 检查孤立记录
   SELECT pe.match_id
   FROM prediction_explanations pe
   LEFT JOIN match_predictions mp ON pe.match_id = mp.match_id
   WHERE mp.match_id IS NULL;
   ```

3. **级联删除**: `ON DELETE CASCADE` 意味着删除预测时，相关的说明也会被删除

## 前端代码调整

如果添加了外键约束，可以恢复嵌套查询：

### SupabaseApi.kt
```kotlin
@GET("match_predictions")
suspend fun getMatchPredictions(
    @Query("select") select: String = "*, prediction_explanations(*)",  // 恢复嵌套查询
    @Query("match_id") matchId: String? = null
): List<PredictionDto>
```

### MatchRepositoryImpl.kt
```kotlin
// 简化代码：不再需要分开查询和手动合并
val predictions = api.getMatchPredictions()
// predictions 已包含嵌套的 predictionExplanations
```

## 性能对比

### 当前方案（分开查询）
- 请求次数：2 次（predictions + explanations）
- 客户端合并：是
- 性能：一般

### 外键方案（嵌套查询）
- 请求次数：1 次
- 后端 JOIN：是
- 性能：更好

## 推荐

如果你有后端访问权限，建议添加外键约束。这样可以：
1. 利用 PostgREST 的嵌套查询功能
2. 减少网络请求
3. 简化前端代码
4. 提高数据一致性
