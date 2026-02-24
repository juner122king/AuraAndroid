# 预测数据获取简化版本（如果嵌套查询有问题）

## 如果遇到 prediction_explanations 相关错误

### 临时方案：只获取预测概率，不获取说明

#### 1. 修改 API 调用（SupabaseApi.kt）

```kotlin
// 单独查询match_predictions（临时简化版）
@GET("match_predictions")
suspend fun getMatchPredictions(
    @Query("select") select: String = "*",  // 不包含嵌套查询
    @Query("match_id") matchId: String? = null
): List<PredictionDto>
```

#### 2. 修改 PredictionDto.toEntity()（EntityMappers.kt）

```kotlin
fun PredictionDto.toEntity(): PredictionEntity {
    return PredictionEntity(
        matchId = matchId,
        modelVersion = modelVersion ?: "v1.0",
        homeWinProb = homeWinProb,
        drawProb = drawProb,
        awayWinProb = awayWinProb,
        confidence = confidence ?: 0.5f,
        // 简化版：使用默认说明
        explanation = "AI预测结果（置信度: ${((confidence ?: 0.5f) * 100).toInt()}%）"
    )
}
```

#### 3. 后续再添加说明

等预测概率能正常显示后，再单独处理 prediction_explanations 的查询。

## 验证步骤

1. 应用上述修改
2. Rebuild 项目
3. 清除应用数据
4. 启动应用
5. 查看是否能看到 "🤖 AI预测可用" 标签

如果能看到，说明基本流程正确，再优化 prediction_explanations 的处理。
