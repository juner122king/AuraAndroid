package com.aura.football.presentation.standings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.football.domain.model.League
import com.aura.football.domain.model.Standing
import com.aura.football.domain.usecase.GetLeaguesUseCase
import com.aura.football.domain.usecase.GetStandingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StandingsViewModel @Inject constructor(
    private val getLeaguesUseCase: GetLeaguesUseCase,
    private val getStandingsUseCase: GetStandingsUseCase
) : ViewModel() {

    private val _leagues = MutableStateFlow<List<League>>(emptyList())
    val leagues: StateFlow<List<League>> = _leagues.asStateFlow()

    private val _selectedLeague = MutableStateFlow<League?>(null)
    val selectedLeague: StateFlow<League?> = _selectedLeague.asStateFlow()

    private val _uiState = MutableStateFlow<StandingsUiState>(StandingsUiState.Loading)
    val uiState: StateFlow<StandingsUiState> = _uiState.asStateFlow()

    init {
        loadLeagues()
    }

    private fun loadLeagues() {
        viewModelScope.launch {
            try {
                val leaguesList = getLeaguesUseCase()
                _leagues.value = leaguesList

                if (leaguesList.isNotEmpty()) {
                    onLeagueSelected(leaguesList[0])
                }
            } catch (e: Exception) {
                _uiState.value = StandingsUiState.Error(e.message ?: "加载联赛失败")
            }
        }
    }

    fun onLeagueSelected(league: League) {
        _selectedLeague.value = league
        loadStandings(league.id)
    }

    private fun loadStandings(leagueId: Long) {
        viewModelScope.launch {
            _uiState.value = StandingsUiState.Loading

            getStandingsUseCase(leagueId)
                .catch { e ->
                    _uiState.value = StandingsUiState.Error(e.message ?: "加载榜单失败")
                }
                .collect { standings ->
                    _uiState.value = if (standings.isEmpty()) {
                        StandingsUiState.Empty
                    } else {
                        StandingsUiState.Success(standings)
                    }
                }
        }
    }
}

sealed class StandingsUiState {
    object Loading : StandingsUiState()
    data class Success(val standings: List<Standing>) : StandingsUiState()
    data class Error(val message: String) : StandingsUiState()
    object Empty : StandingsUiState()
}
