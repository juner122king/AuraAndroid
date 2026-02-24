package com.aura.football.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aura.football.domain.model.Match
import com.aura.football.domain.model.MatchStatus
import com.aura.football.domain.model.PredictionOutcome
import com.aura.football.presentation.theme.FinishedGray
import com.aura.football.presentation.theme.LiveRed
import com.aura.football.presentation.theme.ScheduledBlue
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchCard(
    match: Match,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // League and Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // League emblem
                    match.league.emblemUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Text(
                        text = match.league.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Round number badge
                    if (match.roundNumber != null) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "第${match.roundNumber}轮",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // AI indicator badge
                    if (match.prediction != null) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = match.matchTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Teams and score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home team
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Home team logo
                    if (match.homeTeam.logoUrl != null) {
                        AsyncImage(
                            model = match.homeTeam.logoUrl,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // 占位符 - 显示首字母
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = match.homeTeam.name.take(1),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 英文缩写
                        Text(
                            text = match.homeTeam.shortName ?: match.homeTeam.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // 中文名称
                        Text(
                            text = match.homeTeam.nameZh ?: match.homeTeam.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Score or VS
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (match.score != null) {
                        Text(
                            text = "${match.score.home} - ${match.score.away}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Away team
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 英文缩写
                        Text(
                            text = match.awayTeam.shortName ?: match.awayTeam.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // 中文名称
                        Text(
                            text = match.awayTeam.nameZh ?: match.awayTeam.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        )
                    }

                    // Away team logo
                    if (match.awayTeam.logoUrl != null) {
                        AsyncImage(
                            model = match.awayTeam.logoUrl,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // 占位符 - 显示首字母
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = match.awayTeam.name.take(1),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                MatchStatusChip(status = match.status)
            }

            // AI Prediction Section
            match.prediction?.let { prediction ->
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    // AI预测标题和可信度
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "\uD83E\uDD16",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "AI预测",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // 可信度徽章
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = getConfidenceColor(prediction.confidence).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "可信度 ${(prediction.confidence * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = getConfidenceColor(prediction.confidence),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 三个概率条
                    PredictionBar(
                        label = match.homeTeam.displayName,
                        probability = prediction.homeWinProb,
                        isHighlighted = prediction.mostLikelyOutcome == PredictionOutcome.HOME_WIN,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PredictionBar(
                        label = "平局",
                        probability = prediction.drawProb,
                        isHighlighted = prediction.mostLikelyOutcome == PredictionOutcome.DRAW,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PredictionBar(
                        label = match.awayTeam.displayName,
                        probability = prediction.awayWinProb,
                        isHighlighted = prediction.mostLikelyOutcome == PredictionOutcome.AWAY_WIN,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
fun PredictionBar(
    label: String,
    probability: Float,
    isHighlighted: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val percentage = (probability * 100).roundToInt()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                color = if (isHighlighted) color else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = if (isHighlighted) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isHighlighted) 8.dp else 6.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(probability.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isHighlighted) color else color.copy(alpha = 0.6f)
                    )
            )
        }
    }
}

@Composable
fun MatchStatusChip(
    status: MatchStatus,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (status) {
        MatchStatus.LIVE -> "直播中" to LiveRed
        MatchStatus.FINISHED -> "已结束" to FinishedGray
        MatchStatus.POSTPONED -> "延期" to FinishedGray
        MatchStatus.SCHEDULED -> "未开始" to ScheduledBlue
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun getConfidenceColor(confidence: Float): Color {
    return when {
        confidence >= 0.8f -> Color(0xFF4CAF50)
        confidence >= 0.6f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}
