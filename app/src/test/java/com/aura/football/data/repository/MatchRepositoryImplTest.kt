package com.aura.football.data.repository

import com.aura.football.data.local.dao.LeagueDao
import com.aura.football.data.local.dao.MatchDao
import com.aura.football.data.local.dao.PredictionDao
import com.aura.football.data.local.dao.TeamDao
import com.aura.football.data.remote.SupabaseApi
import com.aura.football.data.remote.dto.MatchPredictionViewDto
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchRepositoryImplTest {

    private lateinit var api: SupabaseApi
    private lateinit var matchDao: MatchDao
    private lateinit var teamDao: TeamDao
    private lateinit var leagueDao: LeagueDao
    private lateinit var predictionDao: PredictionDao
    private lateinit var repository: MatchRepositoryImpl

    @Before
    fun setup() {
        api = mockk()
        matchDao = mockk()
        teamDao = mockk()
        leagueDao = mockk()
        predictionDao = mockk()
        repository = MatchRepositoryImpl(api, matchDao, teamDao, leagueDao, predictionDao)
    }

    @Test
    fun `getMatchById uses aggregated view and returns prediction`() = runTest {
        coEvery { matchDao.getMatchById(1L) } returns null
        coEvery {
            api.getMatchPredictionsFromView(
                select = "*",
                matchId = "eq.1",
                matchTimeGte = null,
                matchTimeLte = null,
                status = null,
                orCondition = null,
                leagueId = null,
                order = "match_time.asc",
                limit = 1,
                offset = null
            )
        } returns listOf(
            MatchPredictionViewDto(
                matchId = 1,
                matchTime = "2026-04-03T12:00:00Z",
                status = "scheduled",
                homeScore = null,
                awayScore = null,
                round = null,
                roundNumber = 30,
                homeTeamId = 10,
                homeTeamName = "Arsenal",
                homeTeamLogoUrl = null,
                awayTeamId = 20,
                awayTeamName = "Chelsea",
                awayTeamLogoUrl = null,
                leagueId = 100,
                leagueName = "Premier League",
                leagueLogoUrl = null,
                country = "England",
                predictionId = 99,
                modelVersion = "v2",
                homeWinProb = 0.55f,
                drawProb = 0.25f,
                awayWinProb = 0.20f,
                confidence = 0.8f,
                explanations = listOf("主队状态更好", "客队客场偏弱")
            )
        )
        coEvery { teamDao.getTeamsByIds(listOf(10L, 20L)) } returns emptyList()
        coEvery { leagueDao.getAllLeagues() } returns emptyList()
        coEvery { teamDao.insertTeams(any()) } just runs
        coEvery { leagueDao.insertLeagues(any()) } just runs
        coEvery { matchDao.insertMatches(any()) } just runs
        coEvery { predictionDao.deletePredictionsByMatchIds(listOf(1L)) } just runs
        coEvery { predictionDao.insertPredictions(any()) } just runs

        val result = repository.getMatchById(1L)

        assertNotNull(result)
        assertNotNull(result?.prediction)
        assertEquals("v2", result?.prediction?.modelVersion)
        assertEquals("主队状态更好\n客队客场偏弱", result?.prediction?.explanation)
    }
}
