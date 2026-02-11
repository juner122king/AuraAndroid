# 预测数据显示验证清单

## ✅ 已确认
- [x] 后端表 `match_predictions` 存在
- [x] 后端表 `prediction_explanations` 存在
- [x] 前端代码已添加预测数据查询逻辑
- [x] DTO 和 Entity 结构匹配

## 📋 验证步骤

### 步骤 1: 检查后端是否有预测数据

在 Supabase SQL Editor 中执行：

```sql
-- 查看预测数据总数
SELECT COUNT(*) FROM match_predictions;

-- 查看最近的预测记录
SELECT
    mp.*,
    m.match_time,
    ht.name as home_team,
    at.name as away_team
FROM match_predictions mp
JOIN matches m ON mp.match_id = m.id
JOIN teams ht ON m.home_team_id = ht.id
JOIN teams at ON m.away_team_id = at.id
ORDER BY m.match_time DESC
LIMIT 5;

-- 查看预测说明
SELECT * FROM prediction_explanations LIMIT 5;
```

**期望结果**：
- 如果返回记录 → 有预测数据 ✅
- 如果返回空 → 需要先生成预测数据 ❌

---

### 步骤 2: 检查 RLS（Row Level Security）权限

在 Supabase 中确认权限策略：

```sql
-- 查看当前的 RLS 策略
SELECT * FROM pg_policies
WHERE tablename IN ('match_predictions', 'prediction_explanations');

-- 如果没有策略，或策略不允许匿名读取，执行以下命令：

-- 允许匿名用户读取预测数据
CREATE POLICY "Enable read access for all users"
ON match_predictions
FOR SELECT
USING (true);

CREATE POLICY "Enable read access for all users"
ON prediction_explanations
FOR SELECT
USING (true);
```

---

### 步骤 3: 测试 API 直接调用

使用 Postman 或浏览器测试 Supabase API：

```
GET https://你的项目.supabase.co/rest/v1/match_predictions?select=*,prediction_explanations(*)&limit=5

Headers:
  apikey: 你的anon key
  Authorization: Bearer 你的anon key
```

**期望响应**：
```json
[
  {
    "match_id": 1,
    "model_version": "v1.0",
    "home_win_prob": 0.45,
    "draw_prob": 0.30,
    "away_win_prob": 0.25,
    "confidence": 0.75,
    "prediction_explanations": [
      {
        "id": 1,
        "prediction_id": 1,
        "explanation_text": "主队近期状态良好..."
      }
    ]
  }
]
```

---

### 步骤 4: 运行 Android 应用并查看日志

#### 4.1 清除应用数据
在设置中清除应用数据，或：
```bash
adb shell pm clear com.aura.football
```

#### 4.2 启动应用
打开应用，进入比赛列表页面

#### 4.3 查看 Logcat 日志

**过滤关键词**: `MatchRepository`

**期望看到的日志**：
```
MatchRepository: 开始网络请求（嵌入式查询）...
MatchRepository: 网络请求成功，收到 X 场完整比赛数据
MatchRepository: 成功获取并缓存 Y 条预测数据
MatchRepository: 成功获取并缓存 X 场比赛数据
```

**如果看到错误**：
```
MatchRepository: 获取预测数据失败: [错误信息]
```
→ 将完整错误信息告诉我，我会帮你诊断

---

### 步骤 5: 使用 Database Inspector 检查本地数据

1. 在 Android Studio 中打开 **App Inspection**
2. 选择 **Database Inspector**
3. 选择设备和进程 `com.aura.football`
4. 查看 `predictions` 表

**期望结果**：
- 表中应该有预测记录
- `match_id` 应该对应 `matches` 表中的记录

---

### 步骤 6: 检查 UI 显示

在比赛卡片上应该看到预测信息。检查 `MatchCard.kt` 是否有渲染预测数据的代码。

让我查看一下 MatchCard：

```kotlin
// 在 MatchCard.kt 中应该有类似的代码：
match.prediction?.let { prediction ->
    // 显示预测概率
    Text("主胜: ${(prediction.homeWinProb * 100).toInt()}%")
    Text("平局: ${(prediction.drawProb * 100).toInt()}%")
    Text("客胜: ${(prediction.awayWinProb * 100).toInt()}%")
}
```

---

## 🔧 常见问题排查

### 问题 1: API 返回 404
**错误**: `{"code":"PGRST116","message":"relation \"match_predictions\" does not exist"}`

**原因**: 表名不匹配或表不存在

**解决**:
- 检查 Supabase Table Editor 中表的准确名称
- 确认是否是 `match_predictions`（复数）还是 `match_prediction`（单数）

---

### 问题 2: API 返回 403 或权限错误
**错误**: `{"code":"42501","message":"permission denied"}`

**原因**: RLS 策略未配置或配置错误

**解决**: 执行步骤 2 中的 RLS 策略创建语句

---

### 问题 3: API 返回空数组
**响应**: `[]`

**原因**: 数据库中没有预测数据

**解决**: 插入测试数据
```sql
-- 获取一些现有的 match_id
SELECT id, match_time FROM matches
WHERE status = 'scheduled'
ORDER BY match_time
LIMIT 5;

-- 为这些比赛插入预测数据（替换下面的 match_id）
INSERT INTO match_predictions (match_id, model_version, home_win_prob, draw_prob, away_win_prob, confidence)
VALUES
  (替换为实际的match_id, 'v1.0', 0.45, 0.30, 0.25, 0.75);

-- 插入预测说明
INSERT INTO prediction_explanations (match_id, explanation_text)
VALUES
  (替换为实际的match_id, '主队近期状态良好，主场作战优势明显。客队近期客场战绩不佳。');
```

---

### 问题 4: 应用日志显示"获取预测数据失败"
可能的原因：
1. 网络连接问题
2. Supabase API key 配置错误
3. API 端点 URL 不正确
4. 响应格式不匹配

**排查步骤**：
- 查看完整的错误堆栈
- 使用网络抓包工具（如 Charles）查看实际的 HTTP 请求和响应
- 确认 `local.properties` 或配置文件中的 Supabase 配置正确

---

### 问题 5: PredictionDto 字段映射错误

如果后端表字段名与 DTO 不一致，需要调整 `@SerializedName`。

**检查后端字段名**：
```sql
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'match_predictions';
```

**常见不匹配**：
- `home_win_prob` vs `home_win_probability`
- `confidence` vs `confidence_score`
- `model_version` vs `version`

---

## 📞 需要支持？

完成上述步骤后，如果仍有问题，请提供：

1. **步骤 1 的 SQL 查询结果**（截图或文本）
2. **步骤 3 的 API 测试结果**（JSON 响应）
3. **步骤 4 的完整 Logcat 日志**（特别是错误信息）
4. **步骤 5 的 Database Inspector 截图**

我会根据这些信息进一步协助调试！
