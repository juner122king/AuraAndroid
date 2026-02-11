package com.aura.football.domain.usecase

import com.aura.football.domain.model.Match
import com.aura.football.domain.model.MatchStatus
import com.aura.football.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * 获取即将到来的比赛
 */
class GetUpcomingMatchesUseCase @Inject constructor(
    private val matchRepository: MatchRepository
) {
    /**
     * @param teamId 球队ID
     * @return 即将到来的比赛列表Flow，按时间升序排列
     */
    operator fun invoke(teamId: Long): Flow<List<Match>> {
        // 查询今天到未来30天的比赛
        val startDate = LocalDate.now().toString()
        val endDate = LocalDate.now().plusDays(30).toString()

        return matchRepository.getMatches(startDate, endDate).map { matches ->
            matches
                .filter { match ->
                    // 过滤出该球队的未开始比赛
                    (match.homeTeam.id == teamId || match.awayTeam.id == teamId) &&
                    match.status == MatchStatus.SCHEDULED
                }
                .sortedBy { it.matchTime } // 按时间升序
        }
    }
}
