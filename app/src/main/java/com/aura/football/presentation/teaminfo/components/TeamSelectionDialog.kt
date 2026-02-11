package com.aura.football.presentation.teaminfo.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.aura.football.domain.model.Team
import com.aura.football.domain.repository.LeagueWithTeams

/**
 * 球队选择对话框（按联赛分组）
 */
@Composable
fun TeamSelectionDialog(
    leaguesWithTeams: List<LeagueWithTeams>,
    selectedTeamId: Long?,
    onTeamSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 记录每个联赛的展开/折叠状态，默认展开第一个
    val expandedStates = remember {
        mutableStateMapOf<Long, Boolean>().apply {
            leaguesWithTeams.forEachIndexed { index, league ->
                put(league.league.id, index == 0)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "选择球队",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                HorizontalDivider()

                // League & Team List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    leaguesWithTeams.forEach { leagueWithTeams ->
                        // League Header (可点击展开/折叠)
                        item(key = "league_${leagueWithTeams.league.id}") {
                            LeagueHeader(
                                leagueName = leagueWithTeams.league.name,
                                teamCount = leagueWithTeams.teams.size,
                                isExpanded = expandedStates[leagueWithTeams.league.id] ?: false,
                                onToggle = {
                                    val currentState = expandedStates[leagueWithTeams.league.id] ?: false
                                    expandedStates[leagueWithTeams.league.id] = !currentState
                                }
                            )
                        }

                        // Teams in this league (with animation)
                        item(key = "teams_${leagueWithTeams.league.id}") {
                            AnimatedVisibility(
                                visible = expandedStates[leagueWithTeams.league.id] ?: false,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column {
                                    leagueWithTeams.teams.forEach { team ->
                                        TeamSelectionItem(
                                            team = team,
                                            isSelected = team.id == selectedTeamId,
                                            onClick = {
                                                onTeamSelected(team.id)
                                                onDismiss()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Footer with close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text("关闭")
                }
            }
        }
    }
}

@Composable
private fun LeagueHeader(
    leagueName: String,
    teamCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (isExpanded) "折叠" else "展开",
                    tint = MaterialTheme.colorScheme.primary
                )

                Column {
                    Text(
                        text = leagueName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$teamCount 支球队",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamSelectionItem(
    team: Team,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Team Logo
            team.logoUrl?.let { logoUrl ->
                AsyncImage(
                    model = logoUrl,
                    contentDescription = "球队队徽",
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Fit
                )
            } ?: run {
                // Placeholder
                Surface(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = team.shortName?.take(2) ?: "?",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Team Name
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = team.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Selection Indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
