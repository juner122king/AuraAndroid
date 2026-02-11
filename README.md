# AuraAPP - 足球赛事查看与AI预测应用

一个基于Android原生开发的足球赛事查看应用，集成AI预测功能。

## 功能特性

- ✅ 查看赛事（今日/明日/本周筛选）
- ✅ 显示AI预测结果（概率+解释）
- ✅ 查看联赛积分榜
- ✅ 比赛详情查看
- ✅ 离线缓存支持
- ✅ 后台自动更新比分

## 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose + Material Design 3
- **架构**: MVVM + Clean Architecture
- **依赖注入**: Hilt
- **网络**: Retrofit + OkHttp
- **数据库**: Room
- **后端**: Supabase
- **异步**: Kotlin Coroutines + Flow
- **后台任务**: WorkManager
- **图片加载**: Coil

## 项目结构

```
app/src/main/java/com/aura/football/
├── data/                    # 数据层
│   ├── local/              # 本地数据库
│   │   ├── dao/            # DAO接口
│   │   └── entity/         # Room实体
│   ├── remote/             # 网络层
│   │   └── dto/            # 数据传输对象
│   └── repository/         # Repository实现
├── domain/                  # 业务逻辑层
│   ├── model/              # 领域模型
│   ├── repository/         # Repository接口
│   └── usecase/            # 用例
├── presentation/            # UI层
│   ├── theme/              # 主题配置
│   ├── common/             # 通用组件
│   ├── home/               # 首页
│   ├── matchdetail/        # 比赛详情
│   ├── standings/          # 联赛榜单
│   └── navigation/         # 导航
└── di/                      # 依赖注入模块
```

## 构建与运行

### 前置要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- 最低支持 Android 7.0 (API 24)

### 构建步骤

1. 克隆项目
```bash
git clone <repository-url>
cd AuraAPP
```

2. 在Android Studio中打开项目

3. 同步Gradle依赖
```bash
./gradlew build
```

4. 运行应用
- 连接Android设备或启动模拟器
- 点击"Run"按钮或执行 `./gradlew installDebug`

## 配置

应用的Supabase配置已在 `build.gradle.kts` 中设置。如需修改，请更新以下字段：

```kotlin
buildConfigField("String", "SUPABASE_URL", "\"your-supabase-url\"")
buildConfigField("String", "SUPABASE_ANON_KEY", "\"your-anon-key\"")
```

## 数据库架构

应用连接到位于 `C:\Users\juner\supabase\Aura` 的Supabase数据库，包含以下主要表：

- `matches` - 比赛信息
- `teams` - 球队信息
- `leagues` - 联赛信息
- `match_predictions` - AI预测结果
- `league_standings` - 联赛积分榜

## 开发计划

### 已完成
- [x] 项目基础架构
- [x] 数据层实现
- [x] 业务逻辑层
- [x] UI组件开发
- [x] 网络配置
- [x] 本地缓存

### 待实现
- [ ] 推送通知
- [ ] 收藏功能
- [ ] 分享功能
- [ ] 多语言支持
- [ ] 更多数据可视化

## 许可证

[MIT License](LICENSE)

## 联系方式

如有问题或建议，请联系开发团队。
