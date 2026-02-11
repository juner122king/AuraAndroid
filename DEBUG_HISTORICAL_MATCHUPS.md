# 历史对局功能调试指南

## 问题描述

用户反馈所有队伍之间都没有历史对局记录。

## 已修复的问题

### 1. Supabase OR过滤器语法问题

**原代码**：使用复杂的OR过滤器，可能不被Supabase PostgREST正确解析
```kotlin
val orFilter = "(and(home_team_id.eq.$homeTeamId,away_team_id.eq.$awayTeamId)," +
               "and(home_team_id.eq.$awayTeamId,away_team_id.eq.$homeTeamId))"
api.getHistoricalMatches(orFilter = orFilter)
```

**新代码**：采用之前成功的模式 - 先获取所有数据，然后在代码中过滤
```kotlin
// 1. 获取所有finished状态的比赛
val allFinishedMatches = api.getMatches(status = "eq.finished")

// 2. 在代码中过滤出两队的历史交锋
val matchesDto = allFinishedMatches.filter { match ->
    (match.homeTeamId == homeTeamId && match.awayTeamId == awayTeamId) ||
    (match.homeTeamId == awayTeamId && match.awayTeamId == homeTeamId)
}
```

### 2. 添加详细日志

新增日志输出：
```kotlin
Log.d(TAG, "数据库中共有 ${allFinishedMatches.size} 场已完赛比赛")
Log.d(TAG, "找到 ${matchesDto.size} 场历史对局")
```

### 3. 添加按时间倒序排序

```kotlin
.sortedByDescending { it.matchTime }
```

### 4. 添加异常处理

即使查询失败也不会崩溃，而是返回空数据：
```kotlin
} catch (e: Exception) {
    Log.e(TAG, "获取历史对局失败: ${e.message}", e)
    // 返回空数据而不是抛出异常
    HistoricalMatchupStats(...)
}
```

## 如何调试

### 方法1: 查看Logcat日志

1. 安装新的APK：
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

2. 启动应用：
```bash
adb shell am start -n com.aura.football/.MainActivity
```

3. 清除旧日志并开始监听：
```bash
adb logcat -c
adb logcat | grep "MatchRepository"
```

4. 在应用中打开任意比赛详情，点击"历史对局"Tab

5. 查看日志输出：
```
MatchRepository: 开始获取历史对局: 123 vs 456
MatchRepository: 数据库中共有 XXX 场已完赛比赛
MatchRepository: 找到 YYY 场历史对局
MatchRepository: 历史对局统计: Z胜 W平 V负, 进球 A:B
```

### 方法2: 检查Supabase数据库

可能的问题：
1. **数据库中没有finished状态的比赛**
   - 检查：`SELECT COUNT(*) FROM matches WHERE status = 'finished'`
   - 如果返回0，说明数据库中所有比赛都是scheduled或live状态

2. **比赛数据没有比分**
   - 检查：`SELECT COUNT(*) FROM matches WHERE status = 'finished' AND (home_score IS NULL OR away_score IS NULL)`
   - 历史对局功能要求比赛必须有比分

3. **team_id不匹配**
   - 检查数据库中的team_id是否与应用中显示的一致

### 方法3: 临时修改代码测试

如果想看到更多调试信息，可以临时修改 `MatchRepositoryImpl.kt`:

```kotlin
// 在 getHistoricalMatchups 方法中添加
Log.d(TAG, "HomeTeamId: $homeTeamId, AwayTeamId: $awayTeamId")

// 在获取所有比赛后添加
allFinishedMatches.take(5).forEach { match ->
    Log.d(TAG, "Sample match: id=${match.id}, home=${match.homeTeamId}, away=${match.awayTeamId}, status=${match.status}")
}

// 在过滤后添加
matchesDto.forEach { match ->
    Log.d(TAG, "历史对局: ${match.homeTeamId} vs ${match.awayTeamId}, 比分 ${match.homeScore}:${match.awayScore}")
}
```

## 预期结果

### 如果数据库有finished比赛

日志应该显示：
```
MatchRepository: 开始获取历史对局: 1 vs 2
MatchRepository: 数据库中共有 150 场已完赛比赛
MatchRepository: 找到 5 场历史对局
MatchRepository: 历史对局统计: 2胜 1平 2负, 进球 8:7
```

UI应该显示统计数据和历史比赛列表。

### 如果数据库没有finished比赛

日志应该显示：
```
MatchRepository: 开始获取历史对局: 1 vs 2
MatchRepository: 数据库中共有 0 场已完赛比赛
MatchRepository: 找到 0 场历史对局
```

UI应该显示："暂无历史交锋记录"

### 如果有finished比赛但两队没交手过

日志应该显示：
```
MatchRepository: 开始获取历史对局: 1 vs 2
MatchRepository: 数据库中共有 150 场已完赛比赛
MatchRepository: 找到 0 场历史对局
```

UI应该显示："暂无历史交锋记录"

## 可能的解决方案

### 如果数据库中确实没有finished比赛

需要：
1. 修改数据导入脚本，将历史比赛的status设置为'finished'
2. 或者在Supabase数据库中手动更新一些历史比赛的状态：
```sql
UPDATE matches
SET status = 'finished'
WHERE match_time < NOW() - INTERVAL '1 day'
  AND home_score IS NOT NULL
  AND away_score IS NOT NULL;
```

### 如果finished比赛有比分为NULL的情况

修改查询条件，在API层面就过滤掉：
```kotlin
val allFinishedMatches = api.getMatches(status = "eq.finished")
    .filter { it.homeScore != null && it.awayScore != null }
```

## 测试用例

可以在Supabase中创建一些测试数据：

```sql
-- 假设team_id 1和2存在，league_id 1存在
INSERT INTO matches (home_team_id, away_team_id, league_id, match_time, status, home_score, away_score)
VALUES
  (1, 2, 1, '2025-01-01 15:00:00', 'finished', 2, 1),
  (2, 1, 1, '2025-02-01 15:00:00', 'finished', 1, 1),
  (1, 2, 1, '2025-03-01 15:00:00', 'finished', 0, 2);
```

然后在应用中查看team 1和team 2的比赛详情，应该能看到3场历史对局。

## 更新记录

- 2026-02-10: 修复Supabase OR过滤器问题，改用客户端过滤
- 2026-02-10: 添加详细日志和异常处理
- 2026-02-10: 添加按时间倒序排序
