package com.aura.football.presentation.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 日期分隔符组件
 * 在时间轴中用于分隔不同日期的比赛，支持sticky header悬停效果
 *
 * @param text 显示的日期文本，如"今天 周一"、"明天 周二"、"12月15日 周三"
 * @param isToday 是否是今天，今天会有特殊样式
 * @param modifier Modifier
 */
@Composable
fun DateSeparator(
    text: String,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    // 使用Surface提供背景色，确保sticky时不透明，覆盖下方内容
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))

            Surface(
                color = if (isToday)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.weight(1f))
        }
    }
}
