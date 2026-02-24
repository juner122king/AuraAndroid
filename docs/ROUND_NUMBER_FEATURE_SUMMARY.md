# 联赛轮数显示功能实现总结

## 功能描述
在赛程列表item和比赛详情页面添加联赛轮数显示,显示格式如 "Matchday 31" 或 "第31轮"。

## 数据结构说明
从Supabase数据库获取的比赛数据包含以下字段:
- `round`: 文本格式的轮次描述,如 "Matchday 31"
- `round_number`: 数字格式的轮次,如 31

## 实现的修改

### 1. 数据模型层 (Domain Model)
**文件**: `app/src/main/java/com/aura/football/domain/model/Match.kt`
- 在 `Match` 数据类中添加了 `round: String?` 和 `roundNumber: Int?` 字段

### 2. 数据库实体层 (Database Entity)
**文件**: `app/src/main/java/com/aura/football/data/local/entity/Entities.kt`
- 在 `MatchEntity` 中添加了 `round` 和 `roundNumber` 字段
- 添加了相应的 `@ColumnInfo` 注解

**文件**: `app/src/main/java/com/aura/football/data/local/AuraDatabase.kt`
- 数据库版本从 2 升级到 3
- 添加了 `MIGRATION_2_3` 迁移脚本,为 matches 表添加 `round` 和 `round_number` 列

**文件**: `app/src/main/java/com/aura/football/di/DatabaseModule.kt`
- 在数据库构建时添加了 `MIGRATION_2_3` 迁移

### 3. 数据传输对象层 (DTO)
**文件**: `app/src/main/java/com/aura/football/data/remote/dto/MatchDto.kt`
- 添加了 `round` 和 `roundNumber` 字段及对应的 `@SerializedName` 注解

**文件**: `app/src/main/java/com/aura/football/data/remote/dto/MatchWithDetailsDto.kt`
- 添加了 `round` 和 `roundNumber` 字段

**文件**: `app/src/main/java/com/aura/football/data/remote/dto/MatchPredictionViewDto.kt`
- 添加了 `round` 和 `roundNumber` 字段

### 4. 数据映射层 (Mappers)
**文件**: `app/src/main/java/com/aura/football/data/remote/dto/Mappers.kt`
- 更新了 `MatchWithDetailsDto.toDomain()` 方法,包含 round 字段映射
- 更新了 `MatchPredictionViewDto.toDomain()` 方法,包含 round 字段映射

**文件**: `app/src/main/java/com/aura/football/data/local/entity/EntityMappers.kt`
- 更新了 `MatchDto.toEntity()` 方法
- 更新了 `MatchWithRelations.toDomain()` 方法

### 5. 数据仓库层 (Repository)
**文件**: `app/src/main/java/com/aura/football/data/repository/MatchRepositoryImpl.kt`
- 更新了网络数据到实体的转换,包含 round 和 roundNumber 字段
- 更新了 `getMatchById()` 方法中的 Match 构造

### 6. UI层 - 赛程列表卡片
**文件**: `app/src/main/java/com/aura/football/presentation/common/MatchCard.kt`
- 在联赛名称后添加了轮次徽章显示
- 轮次徽章使用 `secondaryContainer` 颜色主题
- 只有当 `match.roundNumber` 不为 null 时才显示
- 显示格式: "第X轮"(使用roundNumber字段格式化)

### 7. UI层 - 比赛详情页面
**文件**: `app/src/main/java/com/aura/football/presentation/matchdetail/MatchDetailScreen.kt`

#### 修改位置1: MatchHeader
- 在联赛名称旁边添加轮次徽章
- 使用半透明的 onPrimaryContainer 颜色作为背景
- 显示格式: "第X轮"

#### 修改位置2: MatchInfoTab
- 在"比赛信息"标签页中添加"轮次"行
- 显示格式: `轮次: 第X轮`
- 使用 roundNumber 字段格式化为中文

## UI显示效果

### 赛程列表卡片
```
[英超] [第31轮] [AI]     02-06 20:01
阿森纳 vs 曼联
```

### 比赛详情 - 头部
```
英超 [第31轮]
2026-02-06 20:01
阿森纳 VS 曼联
```

### 比赛详情 - 信息标签
```
联赛: 英超
轮次: 第31轮
时间: 2026-02-06 20:01
状态: 已结束
```

## 数据库迁移
- 旧版本用户升级时会自动执行 MIGRATION_2_3
- 为 matches 表添加 `round TEXT` 和 `round_number INTEGER` 列
- 现有数据的这两个字段为 null,不影响正常显示

## 兼容性
- 所有字段都是可空类型 (`String?`, `Int?`)
- 当数据库中没有轮次数据时,UI 不会显示轮次徽章
- 向后兼容旧数据

## 编译状态
✅ 编译成功 (BUILD SUCCESSFUL)
- 无错误
- 只有少量警告(已存在的弃用警告,不影响功能)

## 测试建议
1. 清除应用数据或卸载重装,测试数据库迁移
2. 检查列表页面的轮次显示
3. 检查详情页面的轮次显示
4. 测试没有 round 数据的比赛是否正常显示(不显示徽章)
