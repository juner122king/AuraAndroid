package com.aura.football.presentation.home

import com.aura.football.domain.model.League
import com.aura.football.domain.model.Match
import com.aura.football.domain.model.MatchStatus
import com.aura.football.domain.model.Team
import com.aura.football.domain.model.TimelineSection
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
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

    @Test
    fun `loadMorePast appends older matches`() = runTest {
        val today = LocalDate.now()
        val initialStart = today.minusDays(7)
        val initialEnd = today.plusDays(7)
        val pastStart = initialStart.minusDays(30)
        val pastEnd = initialStart.minusDays(1)

        val team = Team(1, "Team A", null, null, null, null)
        val league = League(1, "League A", null, null)

        val initialMatches = listOf(
            Match(
                id = 1,
                homeTeam = team,
                awayTeam = team.copy(id = 2, name = "Team B"),
                league = league,
                matchTime = today.atStartOfDay(),
                status = MatchStatus.SCHEDULED,
                score = null,
                prediction = null
            )
        )

        val pastMatches = listOf(
            Match(
                id = 2,
                homeTeam = team.copy(id = 3, name = "Team C"),
                awayTeam = team.copy(id = 4, name = "Team D"),
                league = league,
                matchTime = pastStart.atStartOfDay(),
                status = MatchStatus.FINISHED,
                score = null,
                prediction = null
            )
        )

        coEvery { leagueRepository.getLeagues() } returns listOf(league)
        coEvery { getTimelineMatchesUseCase(any(), any()) } answers {
            val start = firstArg<LocalDate>()
            val end = secondArg<LocalDate>()
            when {
                start == initialStart && end == initialEnd -> flowOf(initialMatches)
                start == pastStart && end == pastEnd -> flowOf(pastMatches)
                else -> flowOf(emptyList())
            }
        }

        viewModel = HomeViewModel(getTimelineMatchesUseCase, leagueRepository)
        advanceUntilIdle()

        viewModel.loadMorePast()
        advanceUntilIdle()

        val state = viewModel.uiState.value as HomeUiState.Success
        assertEquals(2, state.timeline.sections.flatMap { it.matches }.size)
    }

    @Test
    fun `refresh clears cache and reloads initial timeline`() = runTest {
        val team = Team(1, "Team A", null, null, null, null)
        val league = League(1, "League A", null, null)
        var invocationCount = 0

        val firstBatch = listOf(
            Match(
                id = 1,
                homeTeam = team,
                awayTeam = team.copy(id = 2, name = "Team B"),
                league = league,
                matchTime = LocalDateTime.now(),
                status = MatchStatus.SCHEDULED,
                score = null,
                prediction = null
            )
        )

        val secondBatch = listOf(
            Match(
                id = 2,
                homeTeam = team.copy(id = 3, name = "Team C"),
                awayTeam = team.copy(id = 4, name = "Team D"),
                league = league,
                matchTime = LocalDateTime.now().plusDays(1),
                status = MatchStatus.SCHEDULED,
                score = null,
                prediction = null
            )
        )

        coEvery { leagueRepository.getLeagues() } returns listOf(league)
        coEvery { getTimelineMatchesUseCase(any(), any()) } answers {
            invocationCount += 1
            if (invocationCount == 1) flowOf(firstBatch) else flowOf(secondBatch)
        }

        viewModel = HomeViewModel(getTimelineMatchesUseCase, leagueRepository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value as HomeUiState.Success
        val matchIds = state.timeline.sections.flatMap { it.matches }.map { it.id }
        assertEquals(listOf(2L), matchIds)
    }

    @Test
    fun `anchorDate is today when today section exists`() = runTest {
        val today = LocalDate.now()
        coEvery { leagueRepository.getLeagues() } returns emptyList()
        coEvery { getTimelineMatchesUseCase(any(), any()) } returns flowOf(emptyList())
        viewModel = HomeViewModel(getTimelineMatchesUseCase, leagueRepository)

        val sections = listOf(
            createSection(today.minusDays(1)),
            createSection(today, isToday = true),
            createSection(today.plusDays(1))
        )

        val anchor = viewModel.testFindNearestAnchorDate(sections, today)
        assertEquals(today, anchor)
    }

    @Test
    fun `anchorDate picks nearest date when today section missing`() = runTest {
        val today = LocalDate.now()
        coEvery { leagueRepository.getLeagues() } returns emptyList()
        coEvery { getTimelineMatchesUseCase(any(), any()) } returns flowOf(emptyList())
        viewModel = HomeViewModel(getTimelineMatchesUseCase, leagueRepository)

        val sections = listOf(
            createSection(today.minusDays(5)),
            createSection(today.minusDays(1)),
            createSection(today.plusDays(4))
        )

        val anchor = viewModel.testFindNearestAnchorDate(sections, today)
        assertEquals(today.minusDays(1), anchor)
    }

    @Test
    fun `anchorDate chooses future when equidistant`() = runTest {
        val today = LocalDate.now()
        coEvery { leagueRepository.getLeagues() } returns emptyList()
        coEvery { getTimelineMatchesUseCase(any(), any()) } returns flowOf(emptyList())
        viewModel = HomeViewModel(getTimelineMatchesUseCase, leagueRepository)

        val sections = listOf(
            createSection(today.minusDays(1)),
            createSection(today.plusDays(1))
        )

        val anchor = viewModel.testFindNearestAnchorDate(sections, today)
        assertEquals(today.plusDays(1), anchor)
    }

    @Test
    fun `refresh increments autoScrollToken`() = runTest {
        val team = Team(1, "Team A", null, null, null, null)
        val league = League(1, "League A", null, null)
        val matches = listOf(
            Match(
                id = 1,
                homeTeam = team,
                awayTeam = team.copy(id = 2, name = "Team B"),
                league = league,
                matchTime = LocalDateTime.now(),
                status = MatchStatus.SCHEDULED,
                score = null,
                prediction = null
            )
        )

        coEvery { leagueRepository.getLeagues() } returns listOf(league)
        coEvery { getTimelineMatchesUseCase(any(), any()) } returns flowOf(matches)

        viewModel = HomeViewModel(getTimelineMatchesUseCase, leagueRepository)
        advanceUntilIdle()

        val firstState = viewModel.uiState.value as HomeUiState.Success
        val firstToken = firstState.timeline.autoScrollToken

        viewModel.refresh()
        advanceUntilIdle()

        val secondState = viewModel.uiState.value as HomeUiState.Success
        assertTrue(secondState.timeline.autoScrollToken > firstToken)
    }

    @Test
    fun `loadMore does not change autoScrollToken`() = runTest {
        val today = LocalDate.now()
        val initialStart = today.minusDays(7)
        val initialEnd = today.plusDays(7)
        val pastStart = initialStart.minusDays(30)
        val pastEnd = initialStart.minusDays(1)
        val futureStart = initialEnd.plusDays(1)
        val futureEnd = initialEnd.plusDays(30)

        val team = Team(1, "Team A", null, null, null, null)
        val league = League(1, "League A", null, null)

        val initialMatches = listOf(
            Match(1, team, team.copy(id = 2), league, today.atStartOfDay(), MatchStatus.SCHEDULED, null, null)
        )
        val pastMatches = listOf(
            Match(2, team.copy(id = 3), team.copy(id = 4), league, pastStart.atStartOfDay(), MatchStatus.FINISHED, null, null)
        )
        val futureMatches = listOf(
            Match(3, team.copy(id = 5), team.copy(id = 6), league, futureStart.atStartOfDay(), MatchStatus.SCHEDULED, null, null)
        )

        coEvery { leagueRepository.getLeagues() } returns listOf(league)
        coEvery { getTimelineMatchesUseCase(any(), any()) } answers {
            val start = firstArg<LocalDate>()
            val end = secondArg<LocalDate>()
            when {
                start == initialStart && end == initialEnd -> flowOf(initialMatches)
                start == pastStart && end == pastEnd -> flowOf(pastMatches)
                start == futureStart && end == futureEnd -> flowOf(futureMatches)
                else -> flowOf(emptyList())
            }
        }

        viewModel = HomeViewModel(getTimelineMatchesUseCase, leagueRepository)
        advanceUntilIdle()
        val initialToken = (viewModel.uiState.value as HomeUiState.Success).timeline.autoScrollToken
        assertNotNull(initialToken)

        viewModel.loadMorePast()
        advanceUntilIdle()
        val afterPast = (viewModel.uiState.value as HomeUiState.Success).timeline.autoScrollToken
        assertEquals(initialToken, afterPast)

        viewModel.loadMoreFuture()
        advanceUntilIdle()
        val afterFuture = (viewModel.uiState.value as HomeUiState.Success).timeline.autoScrollToken
        assertEquals(initialToken, afterFuture)
    }

    private fun createSection(date: LocalDate, isToday: Boolean = false): TimelineSection {
        return TimelineSection(
            date = date,
            displayText = date.toString(),
            matches = emptyList(),
            isToday = isToday
        )
    }
}
