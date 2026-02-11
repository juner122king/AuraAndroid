package com.aura.football.data.local.dao

import androidx.room.*
import com.aura.football.data.local.entity.TeamEntity

@Dao
interface TeamDao {

    @Query("SELECT * FROM teams WHERE id = :teamId")
    suspend fun getTeamById(teamId: Long): TeamEntity?

    @Query("SELECT * FROM teams")
    suspend fun getAllTeams(): List<TeamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeams(teams: List<TeamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity)

    @Query("DELETE FROM teams")
    suspend fun deleteAll()
}
