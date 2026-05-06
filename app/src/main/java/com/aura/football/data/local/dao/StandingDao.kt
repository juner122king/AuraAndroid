package com.aura.football.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aura.football.data.local.entity.StandingEntity
import com.aura.football.data.local.entity.StandingWithTeam

@Dao
interface StandingDao {

    @Query(
        """
        SELECT
            s.*,
            t.id as tm_id, t.name as tm_name, t.logo_url as tm_logo_url, t.short_name as tm_short_name,
            t.name_zh as tm_name_zh, t.short_name_zh as tm_short_name_zh
        FROM standings s
        INNER JOIN teams t ON s.team_id = t.id
        WHERE s.league_id = :leagueId
        ORDER BY s.position ASC
        """
    )
    suspend fun getStandingsByLeagueId(leagueId: Long): List<StandingWithTeam>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStandings(standings: List<StandingEntity>)

    @Query("DELETE FROM standings WHERE league_id = :leagueId")
    suspend fun deleteStandingsByLeagueId(leagueId: Long)
}
