package com.aura.football.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aura.football.presentation.home.HomeScreen
import com.aura.football.presentation.matchdetail.MatchDetailScreen
import com.aura.football.presentation.standings.StandingsScreen
import com.aura.football.presentation.teaminfo.TeamInfoScreen

@Composable
fun AuraNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                onMatchClick = { matchId ->
                    navController.navigate(Screen.MatchDetail.createRoute(matchId))
                }
            )
        }

        composable(
            route = Screen.MatchDetail.route,
            arguments = listOf(
                navArgument("matchId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getLong("matchId") ?: 0L
            MatchDetailScreen(
                matchId = matchId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainScreen(
    onMatchClick: (Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.values().forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> HomeScreen(
                onMatchClick = onMatchClick,
                modifier = Modifier.padding(paddingValues)
            )
            1 -> HomeScreen(
                onMatchClick = onMatchClick,
                modifier = Modifier.padding(paddingValues)
            )
            2 -> StandingsScreen(
                modifier = Modifier.padding(paddingValues)
            )
            3 -> TeamInfoScreen(
                onMatchClick = onMatchClick,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

enum class BottomNavItem(
    val title: String,
    val icon: ImageVector
) {
    HOME("首页", Icons.Default.Home),
    MATCHES("赛程", Icons.Default.DateRange),
    STANDINGS("榜单", Icons.Default.List),
    TEAM("球队", Icons.Default.Shield)
}

// Extension function for screens with padding
@Composable
fun HomeScreen(
    onMatchClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    HomeScreen(onMatchClick = onMatchClick)
}

@Composable
fun StandingsScreen(
    modifier: Modifier = Modifier
) {
    StandingsScreen()
}
