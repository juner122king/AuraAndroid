package com.aura.football.presentation.home

import com.aura.football.domain.model.League
import com.aura.football.domain.model.Match
import com.aura.football.domain.model.MatchStatus
import com.aura.football.domain.model.Team
import com.aura.football.domain.repository.LeagueRepository
import com.aura.football.domain.usecase.GetTimelineMatchesUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getTimelineMatchesUseCase: GetTimelineMatchesUseCase
    private lateinit var leagueRepository: LeagueRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getTimelineMatchesUseCase = mockk()
        leagueRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        coEvery { leagueRepository.getLeagues() } returns emptyList()
        coEvery { getTimelineMatchesUseCase(any(), any()) } returns flowOf(emptyList())

        viewModel = HomeViewModel(getTimelineMatchesUseCase, leagueRepository)

        assertEquals(HomeUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `loads matches successfully`() = runTest {
        val testTeam = Team(1, "Team A", null, null, null, null)
        val testLeague = League(1, "League A", null, null)
        val matches = listOf(
            Match(
                id = 1,
                homeTeam = testTeam,
                awayTeam = testTeam.copy(id = 2, name = "Team B"),
                league = testLeague,
                matchTime = LocalDateTime.now(),
                status = MatchStatus.SCHEDULED,
                score = null,
                prediction = null
            )
        )

        coEvery { leagueRepository.getLeagues() } returns emptyList()
        coEvery { getTimelineMatchesUseCase(any(), any()) } returns flowOf(matches)

        viewModel = HomeViewModel(getTimelineMatchesUseCase, leagueRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is HomeUiState.Success)
        val state = viewModel.uiState.value as HomeUiState.Success
        assertEquals(1, state.timeline.sections.flatMap { it.matches }.size)
    }

    @Test
    fun `empty matches shows Empty state`() = runTest {
        coEvery { leagueRepository.getLeagues() } returns emptyList()
        coEvery { getTimelineMatchesUseCase(any(), any()) } returns flowOf(emptyList())

        viewModel = HomeViewModel(getTimelineMatchesUseCase, leagueRepository)
        advanceUntilIdle()

        assertEquals(HomeUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `league filter works`() = runTest {
        val league1 = League(1, "League A", null, null)
        val league2 = League(2, "League B", null, null)
        val testTeam = Team(1, "Team A", null, null, null, null)

        val matches = listOf(
            Match(1, testTeam, testTeam.copy(id = 2), league1, LocalDateTime.now(), MatchStatus.SCHEDULED, null, null),
            Match(2, testTeam.copy(id = 3), testTeam.copy(id = 4), league2, LocalDateTime.now(), MatchStatus.SCHEDULED, null, null)
        )

        coEvery { leagueRepository.getLeagues() } returns listOf(league1, league2)
        coEvery { getTimelineMatchesUseCase(any(), any()) } returns flowOf(matches)

        viewModel = HomeViewModel(getTimelineMatchesUseCase, leagueRepository)
        advanceUntilIdle()

        // Filter to only league 1
        viewModel.updateLeagueFilter(setOf(1L))

        val state = viewModel.uiState.value as HomeUiState.Success
        val filteredMatches = state.timeline.sections.flatMap { it.matches }
        assertEquals(1, filteredMatches.size)
        assertEquals(1L, filteredMatches[0].league.id)
    }
}
