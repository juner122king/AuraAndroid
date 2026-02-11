package com.aura.football.domain.usecase

import com.aura.football.domain.model.Match
import com.aura.football.domain.model.MatchStatus
import com.aura.football.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * 获取时间轴比赛数据
 * 支持动态时间范围查询，用于无限滚动的时间轴视图
 */
class GetTimelineMatchesUseCase @Inject constructor(
    private val repository: MatchRepository
) {
    operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<Match>> {
        return repository.getMatches(
            startDate.toString(),
            endDate.toString()
        ).map { matches ->
            matches
                .filter { it.status != MatchStatus.POSTPONED }
                .sortedBy { it.matchTime }
        }
    }
}
