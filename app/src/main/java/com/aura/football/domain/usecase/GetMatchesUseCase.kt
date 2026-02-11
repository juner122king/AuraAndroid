package com.aura.football.domain.usecase

import com.aura.football.domain.model.Match
import com.aura.football.domain.model.MatchStatus
import com.aura.football.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class GetMatchesUseCase @Inject constructor(
    private val repository: MatchRepository
) {
    operator fun invoke(filter: TimeFilter): Flow<List<Match>> {
        val (startDate, endDate) = filter.getDateRange()

        return repository.getMatches(startDate, endDate)
            .map { matches ->
                matches
                    .filter { it.status != MatchStatus.POSTPONED }
                    .sortedBy { it.matchTime }
            }
    }
}

enum class TimeFilter {
    TODAY,
    TOMORROW,
    THIS_WEEK;

    fun getDateRange(): Pair<String, String> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_DATE

        return when (this) {
            TODAY -> {
                val start = today.atStartOfDay().toString()
                val end = today.plusDays(1).atStartOfDay().toString()
                start to end
            }
            TOMORROW -> {
                val tomorrow = today.plusDays(1)
                val start = tomorrow.atStartOfDay().toString()
                val end = tomorrow.plusDays(1).atStartOfDay().toString()
                start to end
            }
            THIS_WEEK -> {
                val start = today.atStartOfDay().toString()
                val end = today.plusDays(7).atStartOfDay().toString()
                start to end
            }
        }
    }
}
