package com.aura.football.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.football.domain.model.League
import com.aura.football.domain.model.Match
import com.aura.football.domain.model.TimelineSection
import com.aura.football.domain.repository.LeagueRepository
import com.aura.football.domain.usecase.GetTimelineMatchesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTimelineMatchesUseCase: GetTimelineMatchesUseCase,
    private val leagueRepository: LeagueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 联赛列表
    private val _leagues = MutableStateFlow<List<League>>(emptyList())
    val leagues: StateFlow<List<League>> = _leagues.asStateFlow()

    // 选中的联赛ID集合 (空集合表示显示全部)
    private val _selectedLeagueIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedLeagueIds: StateFlow<Set<Long>> = _selectedLeagueIds.asStateFlow()

    // 缓存已加载的比赛数据，避免重复加载
    private val matchesCache = mutableMapOf<LocalDate, List<Match>>()

    // 当前已加载的日期范围
    private var earliestLoadedDate: LocalDate? = null
    private var latestLoadedDate: LocalDate? = null

    init {
        Log.d(TAG, "HomeViewModel initialized")
        loadLeagues()
        loadInitialTimeline()
    }

    /**
     * 初始加载：过去7天 + 今天 + 未来7天
     */
    fun loadInitialTimeline() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "开始加载初始时间轴")
                _uiState.value = HomeUiState.Loading

                val today = LocalDate.now()
                val startDate = today.minusDays(7)
                val endDate = today.plusDays(7)

                getTimelineMatchesUseCase(startDate, endDate)
                    .catch { e ->
                        Log.e(TAG, "加载时间轴失败", e)
                        _uiState.value = HomeUiState.Error(
                            e.message ?: "网络连接失败，请检查网络设置"
                        )
                    }
                    .collect { matches ->
                        Log.d(TAG, "收到初始时间轴数据: ${matches.size}场比赛")

                        // 更新缓存
                        cacheMatches(matches)
                        earliestLoadedDate = startDate
                        latestLoadedDate = endDate

                        updateUiState()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "加载时间轴异常", e)
                _uiState.value = HomeUiState.Error("加载失败: ${e.message}")
            }
        }
    }

    /**
     * 加载联赛列表
     */
    private fun loadLeagues() {
        viewModelScope.launch {
            try {
                _leagues.value = leagueRepository.getLeagues()
                Log.d(TAG, "加载联赛列表成功: ${_leagues.value.size}个联赛")
            } catch (e: Exception) {
                Log.e(TAG, "加载联赛列表失败", e)
            }
        }
    }

    /**
     * 更新联赛筛选
     */
    fun updateLeagueFilter(leagueIds: Set<Long>) {
        _selectedLeagueIds.value = leagueIds
        updateUiState()
    }

    /**
     * 更新UI状态（应用筛选）
     */
    private fun updateUiState() {
        val allMatches = getAllCachedMatches()
        val filtered = filterMatchesByLeagues(allMatches)
        val sections = groupMatchesByDate(filtered)

        _uiState.value = if (sections.isEmpty()) {
            HomeUiState.Empty
        } else {
            val currentState = _uiState.value
            val canLoadMorePast = if (currentState is HomeUiState.Success) {
                currentState.timeline.canLoadMorePast
            } else {
                true
            }
            val canLoadMoreFuture = if (currentState is HomeUiState.Success) {
                currentState.timeline.canLoadMoreFuture
            } else {
                true
            }

            HomeUiState.Success(
                TimelineUiState(
                    sections = sections,
                    canLoadMorePast = canLoadMorePast,
                    canLoadMoreFuture = canLoadMoreFuture
                )
            )
        }
    }

    /**
     * 按联赛筛选比赛
     */
    private fun filterMatchesByLeagues(matches: List<Match>): List<Match> {
        if (_selectedLeagueIds.value.isEmpty()) {
            return matches // 空集合表示显示全部
        }
        return matches.filter { it.league.id in _selectedLeagueIds.value }
    }

    /**
     * 加载更多过去的比赛（上拉触发）
     */
    fun loadMorePast() {
        val currentState = _uiState.value
        if (currentState !is HomeUiState.Success ||
            currentState.timeline.isLoadingPast ||
            !currentState.timeline.canLoadMorePast) {
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "开始加载更多过去的比赛")

                // 更新loading状态
                _uiState.value = HomeUiState.Success(
                    currentState.timeline.copy(isLoadingPast = true)
                )

                val earliestDate = earliestLoadedDate ?: LocalDate.now().minusDays(7)
                val startDate = earliestDate.minusDays(30)
                val endDate = earliestDate.minusDays(1)

                // 检查是否已经加载过去1年的数据
                val oneYearAgo = LocalDate.now().minusYears(1)
                val canLoadMore = startDate.isAfter(oneYearAgo)

                getTimelineMatchesUseCase(startDate, endDate)
                    .catch { e ->
                        Log.e(TAG, "加载更多过去比赛失败", e)
                        // 恢复状态
                        _uiState.value = HomeUiState.Success(
                            currentState.timeline.copy(isLoadingPast = false)
                        )
                    }
                    .collect { matches ->
                        Log.d(TAG, "收到更多过去的比赛: ${matches.size}场")

                        // 更新缓存
                        cacheMatches(matches)
                        earliestLoadedDate = startDate

                        val allMatches = getAllCachedMatches()
                        val filtered = filterMatchesByLeagues(allMatches)
                        val sections = groupMatchesByDate(filtered)
                        _uiState.value = HomeUiState.Success(
                            TimelineUiState(
                                sections = sections,
                                isLoadingPast = false,
                                canLoadMorePast = canLoadMore,
                                canLoadMoreFuture = currentState.timeline.canLoadMoreFuture
                            )
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "加载更多过去比赛异常", e)
            }
        }
    }

    /**
     * 加载更多未来的比赛（下滑触发）
     */
    fun loadMoreFuture() {
        val currentState = _uiState.value
        if (currentState !is HomeUiState.Success ||
            currentState.timeline.isLoadingFuture ||
            !currentState.timeline.canLoadMoreFuture) {
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "开始加载更多未来的比赛")

                // 更新loading状态
                _uiState.value = HomeUiState.Success(
                    currentState.timeline.copy(isLoadingFuture = true)
                )

                val latestDate = latestLoadedDate ?: LocalDate.now().plusDays(7)
                val startDate = latestDate.plusDays(1)
                val endDate = latestDate.plusDays(30)

                // 检查是否已经加载未来3个月的数据
                val threeMonthsLater = LocalDate.now().plusMonths(3)
                val canLoadMore = endDate.isBefore(threeMonthsLater)

                getTimelineMatchesUseCase(startDate, endDate)
                    .catch { e ->
                        Log.e(TAG, "加载更多未来比赛失败", e)
                        // 恢复状态
                        _uiState.value = HomeUiState.Success(
                            currentState.timeline.copy(isLoadingFuture = false)
                        )
                    }
                    .collect { matches ->
                        Log.d(TAG, "收到更多未来的比赛: ${matches.size}场")

                        // 更新缓存
                        cacheMatches(matches)
                        latestLoadedDate = endDate

                        val allMatches = getAllCachedMatches()
                        val filtered = filterMatchesByLeagues(allMatches)
                        val sections = groupMatchesByDate(filtered)
                        _uiState.value = HomeUiState.Success(
                            TimelineUiState(
                                sections = sections,
                                isLoadingFuture = false,
                                canLoadMorePast = currentState.timeline.canLoadMorePast,
                                canLoadMoreFuture = canLoadMore
                            )
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "加载更多未来比赛异常", e)
            }
        }
    }

    /**
     * 刷新当前视图
     */
    fun refresh() {
        // 清空缓存，重新加载初始数据
        matchesCache.clear()
        earliestLoadedDate = null
        latestLoadedDate = null
        loadInitialTimeline()
    }

    /**
     * 缓存比赛数据
     */
    private fun cacheMatches(matches: List<Match>) {
        matches.groupBy { it.matchTime.toLocalDate() }
            .forEach { (date, matchList) ->
                matchesCache[date] = matchList
            }
    }

    /**
     * 获取所有缓存的比赛，按时间排序
     */
    private fun getAllCachedMatches(): List<Match> {
        return matchesCache.values
            .flatten()
            .sortedBy { it.matchTime }
    }

    /**
     * 按日期分组比赛
     */
    private fun groupMatchesByDate(matches: List<Match>): List<TimelineSection> {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        return matches
            .groupBy { it.matchTime.toLocalDate() }
            .map { (date, matchList) ->
                val dayOfWeek = when (date.dayOfWeek.value) {
                    1 -> "周一"
                    2 -> "周二"
                    3 -> "周三"
                    4 -> "周四"
                    5 -> "周五"
                    6 -> "周六"
                    7 -> "周日"
                    else -> ""
                }

                TimelineSection(
                    date = date,
                    displayText = when {
                        date == today -> "今天 $dayOfWeek"
                        date == tomorrow -> "明天 $dayOfWeek"
                        date.year == today.year ->
                            "${date.format(DateTimeFormatter.ofPattern("MM月dd日"))} $dayOfWeek"
                        else ->
                            "${date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))} $dayOfWeek"
                    },
                    matches = matchList,
                    isToday = date == today
                )
            }
            .sortedBy { it.date }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}

/**
 * 时间轴UI状态
 */
data class TimelineUiState(
    val sections: List<TimelineSection>,
    val isLoadingPast: Boolean = false,
    val isLoadingFuture: Boolean = false,
    val canLoadMorePast: Boolean = true,
    val canLoadMoreFuture: Boolean = true
)

/**
 * 主页UI状态
 */
sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val timeline: TimelineUiState) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    object Empty : HomeUiState()
}
