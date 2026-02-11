package com.aura.football.presentation.standings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.football.domain.model.League
import com.aura.football.domain.model.Standing
import com.aura.football.presentation.common.EmptyState
import com.aura.football.presentation.common.ErrorState
import com.aura.football.presentation.common.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingsScreen(
    viewModel: StandingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val leagues by viewModel.leagues.collectAsStateWithLifecycle()
    val selectedLeague by viewModel.selectedLeague.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("联赛榜单") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // League selector
            if (leagues.isNotEmpty()) {
                LeagueSelector(
                    leagues = leagues,
                    selectedLeague = selectedLeague,
                    onLeagueSelected = viewModel::onLeagueSelected,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Content
            when (val state = uiState) {
                is StandingsUiState.Loading -> LoadingState()
                is StandingsUiState.Error -> ErrorState(message = state.message)
                is StandingsUiState.Empty -> EmptyState(message = "暂无榜单数据")
                is StandingsUiState.Success -> StandingsTable(
                    standings = state.standings
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueSelector(
    leagues: List<League>,
    selectedLeague: League?,
    onLeagueSelected: (League) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedLeague?.name ?: "选择联赛",
            onValueChange = {},
            readOnly = true,
            label = { Text("联赛") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            leagues.forEach { league ->
                DropdownMenuItem(
                    text = { Text(league.name) },
                    onClick = {
                        onLeagueSelected(league)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StandingsTable(
    standings: List<Standing>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        // Table header
        item {
            StandingsTableHeader()
        }

        // Table rows
        items(
            items = standings,
            key = { it.team.id }
        ) { standing ->
            StandingsRow(standing = standing)
            HorizontalDivider()
        }
    }
}

@Composable
fun StandingsTableHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = "球队",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "赛",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = "胜",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = "平",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = "负",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = "积分",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )
    }
}

@Composable
fun StandingsRow(
    standing: Standing,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = standing.position.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = standing.team.shortName ?: standing.team.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = standing.played.toString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = standing.won.toString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = standing.drawn.toString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = standing.lost.toString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(32.dp)
        )
        Text(
            text = standing.points.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )
    }
}
