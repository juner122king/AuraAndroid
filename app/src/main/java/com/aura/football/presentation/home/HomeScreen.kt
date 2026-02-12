package com.aura.football.presentation.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.football.presentation.common.EmptyState
import com.aura.football.presentation.common.ErrorState
import com.aura.football.presentation.common.LoadingState
import com.aura.football.presentation.common.MatchCard
import com.aura.football.presentation.home.components.DateSeparator
import com.aura.football.presentation.home.components.LeagueFilterDropdown
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMatchClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val leagues by viewModel.leagues.collectAsStateWithLifecycle()
    val selectedLeagueIds by viewModel.selectedLeagueIds.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Aura足球")

                        // 联赛筛选器 - 紧凑版
                        if (leagues.isNotEmpty()) {
                            LeagueFilterDropdown(
                                leagues = leagues,
                                selectedLeagueIds = selectedLeagueIds,
                                onSelectionChange = viewModel::updateLeagueFilter,
                                modifier = Modifier.width(140.dp),
                                compact = true
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> LoadingState()
                is HomeUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { viewModel.refresh() }
                )
                is HomeUiState.Empty -> EmptyState(message = "暂无比赛")
                is HomeUiState.Success -> TimelineMatchList(
                    timeline = state.timeline,
                    onMatchClick = onMatchClick,
                    onLoadMorePast = viewModel::loadMorePast,
                    onLoadMoreFuture = viewModel::loadMoreFuture
                )
            }
        }
    }
}

/**
 * 时间轴比赛列表
 * 支持无限滚动和sticky header日期分隔符
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineMatchList(
    timeline: TimelineUiState,
    onMatchClick: (Long) -> Unit,
    onLoadMorePast: () -> Unit,
    onLoadMoreFuture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // 监听滚动状态，触发加载更多
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map { layoutInfo ->
                val firstVisibleIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val totalItems = layoutInfo.totalItemsCount
                firstVisibleIndex to lastVisibleIndex to totalItems
            }
            .distinctUntilChanged()
            .collect { (indices, totalItems) ->
                val (firstVisibleIndex, lastVisibleIndex) = indices

                // 接近顶部，加载更多过去的比赛
                if (firstVisibleIndex < 3 && timeline.canLoadMorePast) {
                    onLoadMorePast()
                }

                // 接近底部，加载更多未来的比赛
                if (lastVisibleIndex > totalItems - 3 && timeline.canLoadMoreFuture) {
                    onLoadMoreFuture()
                }
            }
    }

    // 初始滚动到今天的位置
    LaunchedEffect(timeline.sections) {
        if (timeline.sections.isEmpty()) return@LaunchedEffect

        val todayIndex = timeline.sections.indexOfFirst { it.isToday }
        if (todayIndex >= 0) {
            // 计算今天section的item索引
            var itemIndex = 0

            // 如果顶部有loading指示器
            if (timeline.isLoadingPast) {
                itemIndex += 1
            }

            // 累加之前的日期分隔符和比赛数量
            for (i in 0 until todayIndex) {
                itemIndex += 1 // 日期分隔符
                itemIndex += timeline.sections[i].matches.size
            }

            // 滚动到今天的日期分隔符
            listState.scrollToItem(itemIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部加载指示器
        if (timeline.isLoadingPast) {
            item(key = "loading_past") {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }

        // 按日期分组的比赛列表（使用stickyHeader实现悬停效果）
        timeline.sections.forEach { section ->
            // 日期分隔符 - 使用stickyHeader实现悬停
            stickyHeader(key = "date_${section.date}") {
                DateSeparator(
                    text = section.displayText,
                    isToday = section.isToday
                )
            }

            // 该日期的所有比赛
            items(
                items = section.matches,
                key = { "match_${it.id}" }
            ) { match ->
                MatchCard(
                    match = match,
                    onClick = { onMatchClick(match.id) }
                )
            }
        }

        // 底部加载指示器
        if (timeline.isLoadingFuture) {
            item(key = "loading_future") {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
