package com.aura.football.presentation.navigation

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object MatchDetail : Screen("match/{matchId}") {
        fun createRoute(matchId: Long) = "match/$matchId"
    }
}
