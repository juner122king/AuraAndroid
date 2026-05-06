package com.aura.football.data.repository

import com.aura.football.data.local.dao.LeagueDao
import com.aura.football.data.local.dao.LeagueTeamDao
import com.aura.football.data.local.dao.TeamDao
import com.aura.football.data.local.entity.LeagueEntity
import com.aura.football.data.local.entity.LeagueTeamRow
import com.aura.football.data.local.entity.TeamEntity
import com.aura.football.data.remote.SupabaseApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeamRepositoryImplTest {

    private lateinit var api: SupabaseApi
    private lateinit var teamDao: TeamDao
    private lateinit var leagueDao: LeagueDao
    private lateinit var leagueTeamDao: LeagueTeamDao
    private lateinit var repository: TeamRepositoryImpl

    @Before
    fun setup() {
        api = mockk()
        teamDao = mockk()
        leagueDao = mockk()
        leagueTeamDao = mockk()
        repository = TeamRepositoryImpl(api, teamDao, leagueDao, leagueTeamDao)
    }

    @Test
    fun `getAllTeamsGroupedByLeague falls back to cached grouping`() = runTest {
        coEvery { api.getMatches(any(), any(), any(), any(), any(), any()) } throws RuntimeException("offline")
        coEvery { leagueTeamDao.getLeagueTeamRows() } returns listOf(
            LeagueTeamRow(
                league = LeagueEntity(1, "Premier League", "England", null),
                team = TeamEntity(10, "Arsenal", null, "ARS", null, null)
            ),
            LeagueTeamRow(
                league = LeagueEntity(1, "Premier League", "England", null),
                team = TeamEntity(11, "Chelsea", null, "CHE", null, null)
            )
        )

        val result = repository.getAllTeamsGroupedByLeague().first()

        assertEquals(1, result.size)
        assertEquals("Premier League", result.first().league.name)
        assertEquals(2, result.first().teams.size)
    }
}
