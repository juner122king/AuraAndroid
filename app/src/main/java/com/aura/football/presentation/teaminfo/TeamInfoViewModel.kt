package com.aura.football.presentation.teaminfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.football.domain.model.Match
import com.aura.football.domain.model.MatchStatus
import com.aura.football.domain.model.Team
import com.aura.football.domain.repository.LeagueWithTeams
import com.aura.football.domain.repository.MatchRepository
import com.aura.football.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * UI状态
 */
sealed class TeamInfoUiState {
    object Loading : TeamInfoUiState()
    data class Success(
        val team: Team?,
        val stats: TeamStats,
        val historicalMatches: List<Match>,
        val upcomingMatches: List<Match>,
        val leaguesWithTeams: List<LeagueWithTeams>,
        val selectedTeamId: Long?
    ) : TeamInfoUiState()
    data class Error(val message: String) : TeamInfoUiState()
}

/**
 * 球队信息ViewModel
 * 优化：只查询一次比赛数据，然后本地计算
 */
@HiltViewModel
class TeamInfoViewModel @Inject constructor(
    private val getAllTeamsUseCase: GetAllTeamsUseCase,
    private val matchRepository: MatchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TeamInfoUiState>(TeamInfoUiState.Loading)
    val uiState: StateFlow<TeamInfoUiState> = _uiState.asStateFlow()

    private val _selectedTeamId = MutableStateFlow<Long?>(null)

    // 缓存所有比赛数据，避免重复查询
    private var allMatchesCache: List<Match>? = null

    init {
        loadAllTeams()
    }

    private fun loadAllTeams() {
        viewModelScope.launch {
            try {
                _uiState.value = TeamInfoUiState.Loading

                getAllTeamsUseCase().collect { leaguesWithTeams ->
                    if (leaguesWithTeams.isEmpty()) {
                        _uiState.value = TeamInfoUiState.Error("暂无球队数据")
                        return@collect
                    }

                    // 如果还没有选择球队，默认选择第一个联赛的第一支球队
                    if (_selectedTeamId.value == null) {
                        val firstTeam = leaguesWithTeams.firstOrNull()?.teams?.firstOrNull()
                        if (firstTeam != null) {
                            _selectedTeamId.value = firstTeam.id
                        } else {
                            _uiState.value = TeamInfoUiState.Error("暂无球队数据")
                            return@collect
                        }
                    }

                    // 加载选中球队的详细信息
                    _selectedTeamId.value?.let { teamId ->
                        loadTeamInfo(teamId, leaguesWithTeams)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = TeamInfoUiState.Error(
                    e.message ?: "加载球队列表失败"
                )
            }
        }
    }

    private fun loadTeamInfo(teamId: Long, leaguesWithTeams: List<LeagueWithTeams>) {
        viewModelScope.launch {
            try {
                // 只查询一次所有比赛数据
                if (allMatchesCache == null) {
                    val startDate = LocalDate.now().minusYears(2).toString()
                    val endDate = LocalDate.now().plusDays(30).toString()

                    matchRepository.getMatchesForTeam(teamId, startDate, endDate).collect { matches ->
                        allMatchesCache = matches
                        processTeamInfo(teamId, leaguesWithTeams, matches)
                    }
                } else {
                    // 使用缓存数据
                    processTeamInfo(teamId, leaguesWithTeams, allMatchesCache!!)
                }
            } catch (e: Exception) {
                _uiState.value = TeamInfoUiState.Error(
                    e.message ?: "加载球队信息失败"
                )
            }
        }
    }

    private fun processTeamInfo(
        teamId: Long,
        leaguesWithTeams: List<LeagueWithTeams>,
        allMatches: List<Match>
    ) {
        // 过滤出该球队的比赛
        val teamMatches = allMatches.filter { match ->
            match.homeTeam.id == teamId || match.awayTeam.id == teamId
        }

        // 计算统计数据（本地计算，非常快）
        val stats = calculateStats(teamMatches, teamId)

        // 分离历史和未来比赛（本地过滤，非常快）
        val historicalMatches = teamMatches
            .filter { it.status == MatchStatus.FINISHED }
            .sortedByDescending { it.matchTime }

        val upcomingMatches = teamMatches
            .filter { it.status == MatchStatus.SCHEDULED }
            .sortedBy { it.matchTime }

        // 找到球队信息
        val team = leaguesWithTeams
            .flatMap { it.teams }
            .find { it.id == teamId }
            ?: historicalMatches.firstOrNull()?.let { match ->
                if (match.homeTeam.id == teamId) match.homeTeam
                else match.awayTeam
            } ?: upcomingMatches.firstOrNull()?.let { match ->
                if (match.homeTeam.id == teamId) match.homeTeam
                else match.awayTeam
            }

        _uiState.value = TeamInfoUiState.Success(
            team = team,
            stats = stats,
            historicalMatches = historicalMatches,
            upcomingMatches = upcomingMatches,
            leaguesWithTeams = leaguesWithTeams,
            selectedTeamId = teamId
        )
    }

    private fun calculateStats(matches: List<Match>, teamId: Long): TeamStats {
        var wins = 0
        var draws = 0
        var losses = 0
        var goalsFor = 0
        var goalsAgainst = 0

        matches
            .filter { it.status == MatchStatus.FINISHED }
            .forEach { match ->
                val score = match.score ?: return@forEach

                val isHomeTeam = match.homeTeam.id == teamId
                val ourScore = if (isHomeTeam) score.home else score.away
                val theirScore = if (isHomeTeam) score.away else score.home

                goalsFor += ourScore
                goalsAgainst += theirScore

                when {
                    ourScore > theirScore -> wins++
                    ourScore < theirScore -> losses++
                    else -> draws++
                }
            }

        return TeamStats(
            wins = wins,
            draws = draws,
            losses = losses,
            goalsFor = goalsFor,
            goalsAgainst = goalsAgainst,
            totalMatches = wins + draws + losses
        )
    }

    fun selectTeam(teamId: Long) {
        val currentState = _uiState.value
        if (currentState is TeamInfoUiState.Success) {
            _selectedTeamId.value = teamId
            // 使用缓存的比赛数据，立即切换
            allMatchesCache?.let { matches ->
                processTeamInfo(teamId, currentState.leaguesWithTeams, matches)
            }
        }
    }

    fun retry() {
        // 清空缓存，重新加载
        allMatchesCache = null
        loadAllTeams()
    }
}
