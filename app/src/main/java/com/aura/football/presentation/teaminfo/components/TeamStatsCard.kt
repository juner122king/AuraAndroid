package com.aura.football.presentation.teaminfo.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.football.domain.usecase.TeamStats

/**
 * 球队统计卡片
 */
@Composable
fun TeamStatsCard(
    stats: TeamStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "整体战绩统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Win/Draw/Loss Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    label = "胜",
                    value = "${stats.wins}",
                    color = Color(0xFF4CAF50) // Green
                )
                StatColumn(
                    label = "平",
                    value = "${stats.draws}",
                    color = Color(0xFFFFA726) // Orange
                )
                StatColumn(
                    label = "负",
                    value = "${stats.losses}",
                    color = Color(0xFFF44336) // Red
                )
            }

            HorizontalDivider()

            // Goals Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    label = "进球",
                    value = "${stats.goalsFor}",
                    color = MaterialTheme.colorScheme.primary
                )
                StatColumn(
                    label = "失球",
                    value = "${stats.goalsAgainst}",
                    color = MaterialTheme.colorScheme.secondary
                )
                StatColumn(
                    label = "净胜球",
                    value = "${if (stats.goalDifference > 0) "+" else ""}${stats.goalDifference}",
                    color = if (stats.goalDifference > 0) Color(0xFF4CAF50)
                           else if (stats.goalDifference < 0) Color(0xFFF44336)
                           else Color.Gray
                )
            }

            HorizontalDivider()

            // Total Matches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "总比赛场次：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${stats.totalMatches}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
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
