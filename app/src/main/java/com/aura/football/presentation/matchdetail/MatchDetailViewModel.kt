package com.aura.football.presentation.matchdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.football.domain.model.HistoricalMatchupStats
import com.aura.football.domain.model.Match
import com.aura.football.domain.usecase.GetHistoricalMatchupsUseCase
import com.aura.football.domain.usecase.GetMatchDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MatchDetailViewModel @Inject constructor(
    private val getMatchDetailUseCase: GetMatchDetailUseCase,
    private val getHistoricalMatchupsUseCase: GetHistoricalMatchupsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchDetailUiState>(MatchDetailUiState.Loading)
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    private val _historicalMatchups = MutableStateFlow<HistoricalMatchupsState>(HistoricalMatchupsState.Loading)
    val historicalMatchups: StateFlow<HistoricalMatchupsState> = _historicalMatchups.asStateFlow()

    fun loadMatchDetail(matchId: Long) {
        viewModelScope.launch {
            _uiState.value = MatchDetailUiState.Loading
            _historicalMatchups.value = HistoricalMatchupsState.Loading

            try {
                val match = getMatchDetailUseCase(matchId)
                if (match != null) {
                    _uiState.value = MatchDetailUiState.Success(match)

                    // Load historical matchups with league context
                    loadHistoricalMatchups(match.homeTeam.id, match.awayTeam.id, match.league.id)
                } else {
                    _uiState.value = MatchDetailUiState.Error("比赛不存在")
                    _historicalMatchups.value = HistoricalMatchupsState.Error("比赛不存在")
                }
            } catch (e: Exception) {
                _uiState.value = MatchDetailUiState.Error(e.message ?: "加载失败")
                _historicalMatchups.value = HistoricalMatchupsState.Error(e.message ?: "加载失败")
            }
        }
    }

    private fun loadHistoricalMatchups(homeTeamId: Long, awayTeamId: Long, leagueId: Long) {
        viewModelScope.launch {
            try {
                val stats = getHistoricalMatchupsUseCase(homeTeamId, awayTeamId, leagueId)
                _historicalMatchups.value = HistoricalMatchupsState.Success(stats)
            } catch (e: Exception) {
                _historicalMatchups.value = HistoricalMatchupsState.Error(e.message ?: "加载历史对局失败")
            }
        }
    }
}

sealed class MatchDetailUiState {
    object Loading : MatchDetailUiState()
    data class Success(val match: Match) : MatchDetailUiState()
    data class Error(val message: String) : MatchDetailUiState()
}

sealed class HistoricalMatchupsState {
    object Loading : HistoricalMatchupsState()
    data class Success(val stats: HistoricalMatchupStats) : HistoricalMatchupsState()
    data class Error(val message: String) : HistoricalMatchupsState()
}
