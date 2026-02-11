package com.aura.football.presentation.teaminfo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.football.presentation.common.ErrorState
import com.aura.football.presentation.common.LoadingState
import com.aura.football.presentation.common.MatchCard
import com.aura.football.presentation.teaminfo.components.TeamHeaderCard
import com.aura.football.presentation.teaminfo.components.TeamSelectionDialog
import com.aura.football.presentation.teaminfo.components.TeamStatsCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamInfoScreen(
    onMatchClick: (Long) -> Unit,
    viewModel: TeamInfoViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showTeamSelection by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("球队信息") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    // Team Selection Button
                    if (uiState is TeamInfoUiState.Success) {
                        IconButton(onClick = { showTeamSelection = true }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "选择球队"
                            )
                        }
                    }

                    // Refresh Button
                    IconButton(onClick = { viewModel.retry() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when (val state = uiState) {
            is TeamInfoUiState.Loading -> LoadingState()
            is TeamInfoUiState.Error -> ErrorState(
                message = state.message,
                onRetry = { viewModel.retry() }
            )
            is TeamInfoUiState.Success -> {
                TeamInfoContent(
                    state = state,
                    onMatchClick = onMatchClick,
                    modifier = Modifier.padding(paddingValues)
                )

                // Team Selection Dialog
                if (showTeamSelection) {
                    TeamSelectionDialog(
                        leaguesWithTeams = state.leaguesWithTeams,
                        selectedTeamId = state.selectedTeamId,
                        onTeamSelected = { teamId ->
                            viewModel.selectTeam(teamId)
                        },
                        onDismiss = { showTeamSelection = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamInfoContent(
    state: TeamInfoUiState.Success,
    onMatchClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Team Header
        item {
            TeamHeaderCard(team = state.team)
        }

        // Stats Card
        item {
            TeamStatsCard(stats = state.stats)
        }

        // Tab Selector
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("即将到来 (${state.upcomingMatches.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("历史比赛 (${state.historicalMatches.size})") }
                )
            }
        }

        // Match List
        when (selectedTab) {
            0 -> {
                // Upcoming Matches
                if (state.upcomingMatches.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无即将到来的比赛",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(
                        items = state.upcomingMatches,
                        key = { it.id }
                    ) { match ->
                        MatchCard(
                            match = match,
                            onClick = { onMatchClick(match.id) }
                        )
                    }
                }
            }
            1 -> {
                // Historical Matches
                if (state.historicalMatches.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无历史比赛记录",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(
                        items = state.historicalMatches,
                        key = { it.id }
                    ) { match ->
                        MatchCard(
                            match = match,
                            onClick = { onMatchClick(match.id) }
                        )
                    }
                }
            }
        }
    }
}
