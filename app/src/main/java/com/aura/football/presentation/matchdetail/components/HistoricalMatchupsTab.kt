package com.aura.football.presentation.matchdetail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.football.domain.model.HistoricalMatch
import com.aura.football.domain.model.HistoricalMatchupStats
import java.time.format.DateTimeFormatter

@Composable
fun HistoricalMatchupsTab(
    stats: HistoricalMatchupStats,
    currentHomeTeamName: String,
    currentAwayTeamName: String,
    modifier: Modifier = Modifier
) {
    if (stats.totalMatches == 0) {
        // Empty state
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无历史交锋记录",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Statistics Summary Card
        item {
            StatisticsSummaryCard(
                stats = stats,
                homeTeamName = currentHomeTeamName,
                awayTeamName = currentAwayTeamName
            )
        }

        // Match History List Header
        item {
            Text(
                text = "历史交锋记录 (${stats.totalMatches}场)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Historical Matches
        items(stats.matches) { match ->
            HistoricalMatchCard(match = match)
        }
    }
}

@Composable
private fun StatisticsSummaryCard(
    stats: HistoricalMatchupStats,
    homeTeamName: String,
    awayTeamName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "对战统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Win/Draw/Loss Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    label = homeTeamName,
                    value = "${stats.homeTeamWins}胜",
                    color = Color(0xFF4CAF50)
                )
                StatColumn(
                    label = "平局",
                    value = "${stats.draws}场",
                    color = Color(0xFFFFA726)
                )
                StatColumn(
                    label = awayTeamName,
                    value = "${stats.awayTeamWins}胜",
                    color = Color(0xFF1976D2)
                )
            }

            HorizontalDivider()

            // Goal Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    label = "$homeTeamName 进球",
                    value = "${stats.homeTeamGoals}",
                    color = MaterialTheme.colorScheme.primary
                )
                StatColumn(
                    label = "$awayTeamName 进球",
                    value = "${stats.awayTeamGoals}",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoricalMatchCard(match: HistoricalMatch) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // League and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = match.league.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = match.matchTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.homeTeam.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${match.homeScore} - ${match.awayScore}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Text(
                    text = match.awayTeam.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}
