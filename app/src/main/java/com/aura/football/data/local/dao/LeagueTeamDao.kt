package com.aura.football.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aura.football.data.local.entity.LeagueTeamCrossRef
import com.aura.football.data.local.entity.LeagueTeamRow

@Dao
interface LeagueTeamDao {

    @Query(
        """
        SELECT
            l.id as lg_id, l.name as lg_name, l.country as lg_country, l.emblem_url as lg_emblem_url,
            t.id as tm_id, t.name as tm_name, t.logo_url as tm_logo_url, t.short_name as tm_short_name,
            t.name_zh as tm_name_zh, t.short_name_zh as tm_short_name_zh
        FROM league_team_cross_refs r
        INNER JOIN leagues l ON r.league_id = l.id
        INNER JOIN teams t ON r.team_id = t.id
        ORDER BY l.name ASC, t.name ASC
        """
    )
    suspend fun getLeagueTeamRows(): List<LeagueTeamRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<LeagueTeamCrossRef>)

    @Query("DELETE FROM league_team_cross_refs")
    suspend fun deleteAll()
}
