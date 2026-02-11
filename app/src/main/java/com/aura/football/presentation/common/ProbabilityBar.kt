package com.aura.football.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.football.presentation.theme.AwayWinBlue
import com.aura.football.presentation.theme.DrawOrange
import com.aura.football.presentation.theme.HomeWinGreen

@Composable
fun ProbabilityBar(
    homeWinProb: Float,
    drawProb: Float,
    awayWinProb: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Probability bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            // Home win
            if (homeWinProb > 0) {
                Box(
                    modifier = Modifier
                        .weight(homeWinProb)
                        .fillMaxHeight()
                        .background(HomeWinGreen),
                    contentAlignment = Alignment.Center
                ) {
                    if (homeWinProb > 0.15f) {
                        Text(
                            text = "${(homeWinProb * 100).toInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // Draw
            if (drawProb > 0) {
                Box(
                    modifier = Modifier
                        .weight(drawProb)
                        .fillMaxHeight()
                        .background(DrawOrange),
                    contentAlignment = Alignment.Center
                ) {
                    if (drawProb > 0.15f) {
                        Text(
                            text = "${(drawProb * 100).toInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // Away win
            if (awayWinProb > 0) {
                Box(
                    modifier = Modifier
                        .weight(awayWinProb)
                        .fillMaxHeight()
                        .background(AwayWinBlue),
                    contentAlignment = Alignment.Center
                ) {
                    if (awayWinProb > 0.15f) {
                        Text(
                            text = "${(awayWinProb * 100).toInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(label = "主胜", color = HomeWinGreen)
            LegendItem(label = "平局", color = DrawOrange)
            LegendItem(label = "客胜", color = AwayWinBlue)
        }
    }
}

@Composable
fun LegendItem(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = MaterialTheme.shapes.extraSmall)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
