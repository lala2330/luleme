package com.luleme.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luleme.domain.model.Record
import com.luleme.domain.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import javax.inject.Inject

data class StatisticsUiState(
    val weekData: Map<DayOfWeek, Int> = emptyMap(),
    val monthData: Map<LocalDate, Int> = emptyMap(),
    val totalCount: Int = 0,
    val maxStreak: Int = 0,
    val averageFrequency: Float = 0f,
    val loading: Boolean = false,
    val currentWeekOffset: Int = 0,
    val currentMonthOffset: Int = 0,
    val weekLabel: String = "本周",
    val monthLabel: String = ""
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val recordRepository: RecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState(loading = true))
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    /**
     * Reload all data based on the current week/month offsets.
     */
    fun loadAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            
            val allRecords = recordRepository.getAllRecords()
            val today = LocalDate.now()
            val currentWeekOffset = _uiState.value.currentWeekOffset
            val currentMonthOffset = _uiState.value.currentMonthOffset

            // Week Data with offset
            val weekTarget = today.plusWeeks(currentWeekOffset.toLong())
            val startOfWeek = weekTarget.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekData = mutableMapOf<DayOfWeek, Int>()
            for (i in 0..6) {
                val date = startOfWeek.plusDays(i.toLong())
                val count = allRecords.count { it.date == date.format(DateTimeFormatter.ISO_DATE) }
                weekData[date.dayOfWeek] = count
            }
            val weekLabel = "${startOfWeek.year}年第${startOfWeek.get(WeekFields.ISO.weekOfYear())}周"

            // Month Data with offset
            val monthTarget = today.plusMonths(currentMonthOffset.toLong())
            val startOfMonth = monthTarget.with(TemporalAdjusters.firstDayOfMonth())
            val lengthOfMonth = monthTarget.lengthOfMonth()
            val monthData = mutableMapOf<LocalDate, Int>()
            for (i in 0 until lengthOfMonth) {
                val date = startOfMonth.plusDays(i.toLong())
                val count = allRecords.count { it.date == date.format(DateTimeFormatter.ISO_DATE) }
                monthData[date] = count
            }
            val monthLabel = "${startOfMonth.year}年${startOfMonth.monthValue}月"

            // All Time Stats (independent of offset)
            val totalCount = allRecords.size
            val maxStreak = calculateMaxStreak(allRecords)
            
            val firstRecord = allRecords.minByOrNull { it.timestamp }
            val average = if (firstRecord != null) {
                val days = ChronoUnit.DAYS.between(LocalDate.parse(firstRecord.date), today) + 1
                val weeks = kotlin.math.ceil(days / 7.0).toFloat()
                totalCount.toFloat() / weeks
            } else {
                0f
            }

            _uiState.value = StatisticsUiState(
                weekData = weekData,
                monthData = monthData,
                totalCount = totalCount,
                maxStreak = maxStreak,
                averageFrequency = average,
                loading = false,
                currentWeekOffset = currentWeekOffset,
                currentMonthOffset = currentMonthOffset,
                weekLabel = weekLabel,
                monthLabel = monthLabel
            )
        }
    }

    fun navigateWeek(offset: Int) {
        _uiState.value = _uiState.value.copy(currentWeekOffset = offset)
        loadAllData()
    }

    fun navigateMonth(offset: Int) {
        _uiState.value = _uiState.value.copy(currentMonthOffset = offset)
        loadAllData()
    }

    private fun calculateMaxStreak(records: List<Record>): Int {
        if (records.isEmpty()) return 0
        
        val sortedDates = records.map { LocalDate.parse(it.date) }.distinct().sorted()
        var maxStreak = 0
        var currentStreak = 0
        
        for (i in 0 until sortedDates.size) {
            if (i == 0) {
                currentStreak = 1
            } else {
                val prev = sortedDates[i - 1]
                val curr = sortedDates[i]
                if (ChronoUnit.DAYS.between(prev, curr) == 1L) {
                    currentStreak++
                } else {
                    maxStreak = maxOf(maxStreak, currentStreak)
                    currentStreak = 1
                }
            }
        }
        maxStreak = maxOf(maxStreak, currentStreak)
        
        return maxStreak
    }
}
