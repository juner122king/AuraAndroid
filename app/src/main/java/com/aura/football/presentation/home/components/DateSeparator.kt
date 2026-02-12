package com.aura.football.presentation.home.components

import androidx.compose.foundation.background
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
    // 使用Box提供背景色，确保sticky时不透明，覆盖下方内容
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 左侧竖条指示器
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(
                        color = if (isToday)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.extraSmall
                    )
            )

            // 日期文本
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isToday)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 底部细线分隔
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

