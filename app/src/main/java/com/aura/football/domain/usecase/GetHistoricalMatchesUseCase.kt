package com.aura.football.domain.usecase

import com.aura.football.domain.model.Match
import com.aura.football.domain.model.MatchStatus
import com.aura.football.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * 获取历史比赛记录
 */
class GetHistoricalMatchesUseCase @Inject constructor(
    private val matchRepository: MatchRepository
) {
    /**
     * @param teamId 球队ID
     * @return 历史比赛列表Flow，按时间降序排列
     */
    operator fun invoke(teamId: Long): Flow<List<Match>> {
        // 查询过去2年的比赛数据
        val startDate = LocalDate.now().minusYears(2).toString()
        val endDate = LocalDate.now().toString()

        return matchRepository.getMatches(startDate, endDate).map { matches ->
            matches
                .filter { match ->
                    // 过滤出该球队的已完成比赛
                    (match.homeTeam.id == teamId || match.awayTeam.id == teamId) &&
                    match.status == MatchStatus.FINISHED
                }
                .sortedByDescending { it.matchTime } // 按时间降序
        }
    }
}
