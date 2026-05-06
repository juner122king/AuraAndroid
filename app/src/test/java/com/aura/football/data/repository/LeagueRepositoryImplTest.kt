package com.aura.football.data.repository

import com.aura.football.data.local.dao.LeagueDao
import com.aura.football.data.local.dao.StandingDao
import com.aura.football.data.local.dao.TeamDao
import com.aura.football.data.local.entity.StandingEntity
import com.aura.football.data.local.entity.StandingWithTeam
import com.aura.football.data.local.entity.TeamEntity
import com.aura.football.data.remote.SupabaseApi
import com.aura.football.data.remote.dto.StandingDto
import com.aura.football.data.remote.dto.TeamDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeagueRepositoryImplTest {

    private lateinit var api: SupabaseApi
    private lateinit var leagueDao: LeagueDao
    private lateinit var teamDao: TeamDao
    private lateinit var standingDao: StandingDao
    private lateinit var repository: LeagueRepositoryImpl

    @Before
    fun setup() {
        api = mockk()
        leagueDao = mockk()
        teamDao = mockk()
        standingDao = mockk()
        repository = LeagueRepositoryImpl(api, leagueDao, teamDao, standingDao)
    }

    @Test
    fun `getStandings caches network response`() = runTest {
        val dto = standingDto()

        coEvery { api.getStandings(any(), "eq.1", any()) } returns listOf(dto)
        coEvery { teamDao.insertTeams(any()) } just runs
        coEvery { standingDao.deleteStandingsByLeagueId(1L) } just runs
        coEvery { standingDao.insertStandings(any()) } just runs

        val result = repository.getStandings(1L).first()

        assertEquals(1, result.size)
        assertEquals("Arsenal", result.first().team.name)
        coVerify { teamDao.insertTeams(any()) }
        coVerify { standingDao.insertStandings(any()) }
    }

    @Test
    fun `getStandings falls back to cache when network fails`() = runTest {
        coEvery { api.getStandings(any(), "eq.1", any()) } throws RuntimeException("offline")
        coEvery { standingDao.getStandingsByLeagueId(1L) } returns listOf(
            StandingWithTeam(
                standing = StandingEntity(
                    leagueId = 1,
                    teamId = 10,
                    position = 1,
                    played = 20,
                    won = 15,
                    drawn = 3,
                    lost = 2,
                    goalsFor = 40,
                    goalsAgainst = 18,
                    goalDifference = 22,
                    points = 48
                ),
                team = TeamEntity(
                    id = 10,
                    name = "Cached Team",
                    logoUrl = null,
                    shortName = "CT",
                    nameZh = null,
                    shortNameZh = null
                )
            )
        )

        val result = repository.getStandings(1L).first()

        assertEquals(1, result.size)
        assertEquals("Cached Team", result.first().team.name)
    }

    private fun standingDto(): StandingDto {
        return StandingDto(
            position = 1,
            teamId = 10,
            team = TeamDto(
                id = 10,
                name = "Arsenal",
                logoUrl = null,
                shortName = "ARS",
                nameZh = null,
                shortNameZh = null
            ),
            played = 20,
            won = 15,
            drawn = 3,
            lost = 2,
            goalsFor = 40,
            goalsAgainst = 18,
            goalDifference = 22,
            points = 48
        )
    }
}
