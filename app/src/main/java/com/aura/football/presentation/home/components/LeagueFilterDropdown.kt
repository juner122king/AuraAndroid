package com.aura.football.presentation.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.football.domain.model.League

/**
 * 联赛筛选下拉菜单组件
 * 支持多选模式
 * @param compact 紧凑模式，适配TopAppBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueFilterDropdown(
    leagues: List<League>,
    selectedLeagueIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    // 显示文本逻辑
    val displayText = when {
        selectedLeagueIds.isEmpty() -> "全部联赛"
        selectedLeagueIds.size == 1 -> {
            leagues.find { it.id == selectedLeagueIds.first() }?.name ?: "全部联赛"
        }
        selectedLeagueIds.size == leagues.size -> "全部联赛"
        else -> "${selectedLeagueIds.size}个联赛"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        if (compact) {
            // 紧凑模式 - 适配TopAppBar
            Surface(
                onClick = { expanded = true },
                modifier = Modifier.menuAnchor(),
                color = Color.White.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "展开",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            // 标准模式
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                onClick = { expanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            // "全部联赛"选项
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = selectedLeagueIds.isEmpty() || selectedLeagueIds.size == leagues.size,
                            onCheckedChange = null
                        )
                        Text("全部联赛")
                    }
                },
                onClick = {
                    onSelectionChange(emptySet())
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // 各联赛选项
            leagues.forEach { league ->
                val isSelected = selectedLeagueIds.contains(league.id)

                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
                            )
                            Text(league.name)
                        }
                    },
                    onClick = {
                        val newSelection = if (isSelected) {
                            selectedLeagueIds - league.id
                        } else {
                            selectedLeagueIds + league.id
                        }
                        onSelectionChange(newSelection)
                    }
                )
            }
        }
    }
}
