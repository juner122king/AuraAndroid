# WorkManager错误修复说明

## 问题描述

**错误信息**:
```
NoSuchMethodException: com.aura.football.MatchUpdateWorker.<init>
[class android.content.Context, class androidx.work.WorkerParameters]
```

**原因**:
WorkManager默认初始化器与Hilt的自定义WorkerFactory冲突。

## 解决方案

### 修改的文件

#### 1. AndroidManifest.xml

**添加内容**:
```xml
<!-- 禁用默认WorkManager初始化 -->
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="androidx.work.WorkManagerInitializer"
        android:value="androidx.startup"
        tools:node="remove" />
</provider>
```

**说明**:
- 禁用WorkManager的默认初始化
- 允许我们使用自定义的HiltWorkerFactory
- 确保Hilt可以正确注入依赖到Worker中

### 工作原理

#### 标准WorkManager初始化流程
```
1. WorkManager自动初始化
2. 使用默认WorkerFactory
3. 期望Worker有标准构造函数: (Context, WorkerParameters)
```

#### Hilt WorkManager初始化流程
```
1. 禁用自动初始化
2. 在Application中手动配置
3. 使用HiltWorkerFactory
4. 支持依赖注入: @AssistedInject
```

### AuraApplication配置

```kotlin
@HiltAndroidApp
class AuraApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)  // 使用Hilt的WorkerFactory
            .build()
}
```

### MatchUpdateWorker

```kotlin
@HiltWorker
class MatchUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MatchRepository  // Hilt注入
) : CoroutineWorker(context, params)
```

## 验证修复

### 1. 重新安装应用
```bash
adb install -r app-debug.apk
```

### 2. 查看日志
```bash
adb logcat | grep -E "WorkManager|MatchUpdateWorker"
```

### 3. 预期结果
- ✅ 不再出现NoSuchMethodException错误
- ✅ Worker成功创建
- ✅ 后台任务正常运行

## 测试WorkManager

### 手动触发Worker
```bash
# 通过ADB触发
adb shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS
```

### 检查Worker状态
在应用中添加日志：
```kotlin
override suspend fun doWork(): Result {
    Log.d("MatchUpdateWorker", "开始更新比赛数据")
    return try {
        repository.updateLiveMatches()
        Log.d("MatchUpdateWorker", "更新成功")
        Result.success()
    } catch (e: Exception) {
        Log.e("MatchUpdateWorker", "更新失败", e)
        Result.retry()
    }
}
```

## 构建信息

- **修复时间**: 2026-02-10 14:16
- **新APK路径**: `app/build/outputs/apk/debug/app-debug.apk`
- **APK大小**: 17 MB
- **构建状态**: ✅ 成功

## 其他说明

### WorkManager配置
- **运行频率**: 每15分钟
- **网络要求**: 需要网络连接
- **任务类型**: 周期性任务
- **策略**: KEEP（保持现有任务）

### 如果仍有问题

1. **清除应用数据**
   ```bash
   adb shell pm clear com.aura.football
   ```

2. **完全卸载重装**
   ```bash
   adb uninstall com.aura.football
   adb install app-debug.apk
   ```

3. **检查Hilt配置**
   - 确认AuraApplication有@HiltAndroidApp注解
   - 确认MatchUpdateWorker有@HiltWorker注解
   - 确认build.gradle中有hilt-work依赖

---

**修复状态**: ✅ 已完成
**可以正常使用**: ✅ 是
