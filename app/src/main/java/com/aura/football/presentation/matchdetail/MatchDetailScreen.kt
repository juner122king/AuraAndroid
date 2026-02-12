package com.aura.football.presentation.matchdetail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.football.domain.model.Match
import com.aura.football.presentation.common.ErrorState
import com.aura.football.presentation.common.LoadingState
import com.aura.football.presentation.common.ProbabilityBar
import com.aura.football.presentation.matchdetail.components.HistoricalMatchupsTab
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    matchId: Long,
    onNavigateBack: () -> Unit,
    viewModel: MatchDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historicalMatchupsState by viewModel.historicalMatchups.collectAsStateWithLifecycle()

    LaunchedEffect(matchId) {
        viewModel.loadMatchDetail(matchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("比赛详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is MatchDetailUiState.Loading -> LoadingState()
            is MatchDetailUiState.Error -> ErrorState(message = state.message)
            is MatchDetailUiState.Success -> {
                MatchDetailContent(
                    match = state.match,
                    historicalMatchupsState = historicalMatchupsState,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun MatchDetailContent(
    match: Match,
    historicalMatchupsState: HistoricalMatchupsState,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("比赛信息", "AI预测", "球队对比", "历史对局")

    Column(modifier = modifier.fillMaxSize()) {
        // Match header
        MatchHeader(match = match)

        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        // Tab content
        when (selectedTab) {
            0 -> MatchInfoTab(match = match)
            1 -> PredictionTab(match = match)
            2 -> TeamComparisonTab(match = match)
            3 -> {
                when (val state = historicalMatchupsState) {
                    is HistoricalMatchupsState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is HistoricalMatchupsState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    is HistoricalMatchupsState.Success -> {
                        HistoricalMatchupsTab(
                            stats = state.stats,
                            currentHomeTeamName = match.homeTeam.name,
                            currentAwayTeamName = match.awayTeam.name
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatchHeader(
    match: Match,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // League and round
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = match.league.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // Round number badge
                if (match.roundNumber != null) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "第${match.roundNumber}轮",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = match.matchTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Teams and score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home team
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = match.homeTeam.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Score
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (match.score != null) {
                        Text(
                            text = "${match.score.home} - ${match.score.away}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Away team
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = match.awayTeam.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun MatchInfoTab(
    match: Match,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoRow(label = "联赛", value = match.league.name)
        if (match.roundNumber != null) {
            InfoRow(label = "轮次", value = "第${match.roundNumber}轮")
        }
        InfoRow(label = "时间", value = match.matchTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
        InfoRow(label = "状态", value = when (match.status) {
            com.aura.football.domain.model.MatchStatus.SCHEDULED -> "未开始"
            com.aura.football.domain.model.MatchStatus.LIVE -> "直播中"
            com.aura.football.domain.model.MatchStatus.FINISHED -> "已结束"
            com.aura.football.domain.model.MatchStatus.POSTPONED -> "延期"
        })
        if (match.score != null) {
            InfoRow(label = "比分", value = "${match.score.home} - ${match.score.away}")
        }
    }
}

@Composable
fun PredictionTab(
    match: Match,
    modifier: Modifier = Modifier
) {
    val prediction = match.prediction

    if (prediction == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无AI预测",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Probability bar
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "胜负概率",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ProbabilityBar(
                        homeWinProb = prediction.homeWinProb,
                        drawProb = prediction.drawProb,
                        awayWinProb = prediction.awayWinProb
                    )
                }
            }

            // Confidence
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "预测可信度",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { prediction.confidence },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${(prediction.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Explanation
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "预测分析",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = prediction.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "模型版本: ${prediction.modelVersion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TeamComparisonTab(
    match: Match,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "球队信息",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                TeamInfoRow(label = "主队", value = match.homeTeam.name)
                TeamInfoRow(label = "客队", value = match.awayTeam.name)
            }
        }

        Text(
            text = "更多对比数据开发中...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TeamInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
