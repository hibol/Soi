package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.entity.Emotion
import com.hibol.miette.soi.data.entity.EmotionTrendPoint
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.data.entity.EntryType
import com.hibol.miette.soi.data.entity.GlobalStats
import com.hibol.miette.soi.data.entity.MemoryQualityCount
import com.hibol.miette.soi.data.entity.PartCount
import com.hibol.miette.soi.data.entity.PeriodStats
import com.hibol.miette.soi.data.entity.TagCount
import com.hibol.miette.soi.data.entity.TopSecondaryEmotion
import com.hibol.miette.soi.data.repository.CycleDayRepository
import com.hibol.miette.soi.data.repository.EmotionRepository
import com.hibol.miette.soi.data.repository.EntryRepository
import com.hibol.miette.soi.data.repository.PartRepository
import com.hibol.miette.soi.data.repository.ProfileRepository
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class Period(val days: Int, val label: String) {
    WEEK(7, "7 jours"),
    MONTH(30, "30 jours"),
    THREE_MONTHS(90, "3 mois")
}

data class SelectedCell(val col: Int, val primaryEmotionId: Long)

data class TooltipEmotion(val label: String, val color: String, val maxIntensity: Int)

sealed class TooltipUiState {
    object Hidden : TooltipUiState()
    data class Visible(
        val primaryLabel: String,
        val colLabel: String,
        val emotions: List<TooltipEmotion>
    ) : TooltipUiState()
}

sealed class HeatmapUiState {
    object Loading : HeatmapUiState()
    data class Empty(val period: Period) : HeatmapUiState()
    data class Ready(
        // (colIndex, primaryEmotionId) → intensité maximale
        val cells: Map<Pair<Int, Long>, Float>,
        val period: Period,
        val primaryEmotions: List<Emotion>,   // triées par label — ordre stable des lignes Y
        val columnCount: Int,
        val startDate: LocalDate,             // jour-0 de la période (fuseau local)
        val profileId: Long,                  // réutilisé par le tooltip
        val entryTypes: Set<EntryType>        // réutilisé par le tooltip
    ) : HeatmapUiState()
}

private data class HeatmapInputs(
    val period: Period,
    val types: Set<EntryType>,
    val profileId: Long?,
    val allEmotions: List<Emotion>
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExplorationViewModel(
    private val profileRepository: ProfileRepository,
    private val entryRepository: EntryRepository,
    private val emotionRepository: EmotionRepository,
    private val partRepository: PartRepository,
    private val cycleDayRepository: CycleDayRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(Period.WEEK)
    val selectedPeriod: StateFlow<Period> = _selectedPeriod.asStateFlow()

    private val _selectedEntryTypes = MutableStateFlow(
        setOf(EntryType.DREAM, EntryType.SESSION, EntryType.LIFE_EVENT)
    )
    val selectedEntryTypes: StateFlow<Set<EntryType>> = _selectedEntryTypes.asStateFlow()

    private val _selectedCell = MutableStateFlow<SelectedCell?>(null)
    val selectedCell: StateFlow<SelectedCell?> = _selectedCell.asStateFlow()

    fun selectPeriod(period: Period) {
        _selectedPeriod.value = period
        _selectedCell.value = null
    }

    fun toggleEntryType(type: EntryType) {
        val current = _selectedEntryTypes.value
        _selectedEntryTypes.value = when {
            type in current && current.size > 1 -> current - type
            type !in current -> current + type
            else -> current
        }
        _selectedCell.value = null
    }

    fun pressCell(col: Int, primaryEmotionId: Long) {
        _selectedCell.value = SelectedCell(col, primaryEmotionId)
    }

    fun releaseCell() {
        _selectedCell.value = null
    }

    // ─── Statistiques globales (toute la durée de vie du journal) ────────────

    val globalStats: StateFlow<GlobalStats?> = profileRepository.getProfile()
        .flatMapLatest { profile ->
            if (profile == null) return@flatMapLatest flowOf(null)
            entryRepository.getAllByProfile(profile.id).map { entries ->
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now()
                val firstDate = entries.minByOrNull { it.entryDate }
                    ?.let { Instant.ofEpochMilli(it.entryDate).atZone(zone).toLocalDate() }
                val activeSinceDays = if (firstDate != null)
                    ChronoUnit.DAYS.between(firstDate, today) else 0L
                val (current, record) = computeStreaks(entries, zone, today)
                GlobalStats(
                    totalEntries = entries.size,
                    activeSinceDays = activeSinceDays,
                    currentStreak = current,
                    recordStreak = record
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ─── Statistiques de période (réactives au sélecteur) ────────────────────

    val periodStats: StateFlow<PeriodStats?> = combine(
        _selectedPeriod,
        profileRepository.getProfile()
    ) { period, profile -> period to profile }
        .flatMapLatest { (period, profile) ->
            if (profile == null) return@flatMapLatest flowOf(null)
            val zone = ZoneId.systemDefault()
            val fromMillis = LocalDate.now(zone).minusDays((period.days - 1).toLong())
                .atStartOfDay(zone).toInstant().toEpochMilli()
            val now = System.currentTimeMillis()
            combine(
                entryRepository.getByDateRange(profile.id, fromMillis, now),
                entryRepository.getMemoryQualityCounts(profile.id, fromMillis, now),
                entryRepository.getTopTags(profile.id, fromMillis, now),
                partRepository.getTopExplicitParts(profile.id, fromMillis, now)
            ) { entries, memQuality, tags, parts ->
                val zone = ZoneId.systemDefault()
                val weeks = period.days / 7.0f
                val byType = entries.groupingBy { entry ->
                    EntryType.values().find { it.value == entry.entryType }
                }.eachCount().filterKeys { it != null }.mapKeys { it.key!! }
                val topDow = entries
                    .groupingBy { Instant.ofEpochMilli(it.entryDate).atZone(zone).dayOfWeek.value }
                    .eachCount().maxByOrNull { it.value }?.key
                PeriodStats(
                    byType = byType,
                    avgPerWeek = if (weeks > 0f) entries.size / weeks else 0f,
                    topDayOfWeek = topDow,
                    memoryQuality = memQuality,
                    topTags = tags,
                    topParts = parts
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun computeStreaks(entries: List<Entry>, zone: ZoneId, today: LocalDate): Pair<Int, Int> {
        val dates = entries.map {
            Instant.ofEpochMilli(it.entryDate).atZone(zone).toLocalDate()
        }.toSet()
        if (dates.isEmpty()) return 0 to 0
        var current = 0
        var d = if (today in dates) today else today.minusDays(1)
        while (d in dates) { current++; d = d.minusDays(1) }
        val sorted = dates.sorted()
        var record = 1; var run = 1
        for (i in 1 until sorted.size) {
            run = if (sorted[i] == sorted[i - 1].plusDays(1)) run + 1 else 1
            if (run > record) record = run
        }
        return current to record
    }

    val heatmapUiState: StateFlow<HeatmapUiState> = combine(
        _selectedPeriod,
        _selectedEntryTypes,
        profileRepository.getProfile(),
        emotionRepository.getAllEmotions()
    ) { period, types, profile, emotions ->
        HeatmapInputs(period, types, profile?.id, emotions)
    }.flatMapLatest { inputs ->
        if (inputs.profileId == null) return@flatMapLatest flowOf(HeatmapUiState.Loading)
        val zone = ZoneId.systemDefault()
        val startDate = LocalDate.now(zone).minusDays((inputs.period.days - 1).toLong())
        val fromMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        entryRepository.getHeatmapData(inputs.profileId, fromMillis, now, inputs.types.map { it.value })
            .map { points ->
                buildHeatmapState(points, inputs.allEmotions, inputs.period, startDate, inputs.profileId, inputs.types)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeatmapUiState.Loading)

    val heatmapCycleDays: StateFlow<Set<LocalDate>> = combine(
        heatmapUiState,
        profileRepository.getProfile()
    ) { state, profile -> state to profile }
        .flatMapLatest { (state, profile) ->
            if (state !is HeatmapUiState.Ready || profile == null) return@flatMapLatest flowOf(emptySet())
            val endDate = state.startDate.plusDays((state.columnCount - 1).toLong())
            cycleDayRepository.getForRange(profile.id, state.startDate, endDate)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Combine uniquement sur _selectedCell + heatmapUiState (déjà agrégé) — pas de 5e flow
    val tooltipUiState: StateFlow<TooltipUiState> = combine(
        _selectedCell,
        heatmapUiState
    ) { cell, heatmap -> cell to heatmap }
    .flatMapLatest { (cell, heatmap) ->
        if (cell == null || heatmap !is HeatmapUiState.Ready) {
            return@flatMapLatest flowOf(TooltipUiState.Hidden)
        }
        val (fromMillis, toMillis) = cellTimeRange(cell.col, heatmap.startDate, heatmap.period)
        entryRepository.getTopSecondaryEmotions(
            heatmap.profileId, fromMillis, toMillis,
            heatmap.entryTypes.map { it.value }, cell.primaryEmotionId
        ).map { secondaries ->
            val primary = heatmap.primaryEmotions.find { it.id == cell.primaryEmotionId }
            val primaryLabel = primary?.label ?: ""
            val primaryColor = primary?.color ?: "#888888"
            if (secondaries.isEmpty()) TooltipUiState.Hidden
            else TooltipUiState.Visible(
                primaryLabel = primaryLabel,
                colLabel = buildColLabel(cell.col, heatmap.startDate, heatmap.period),
                emotions = secondaries.map { e -> TooltipEmotion(e.label, primaryColor, e.maxIntensity) }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TooltipUiState.Hidden)

    private fun buildHeatmapState(
        points: List<EmotionTrendPoint>,
        allEmotions: List<Emotion>,
        period: Period,
        startDate: LocalDate,
        profileId: Long,
        types: Set<EntryType>
    ): HeatmapUiState {
        val primaries = allEmotions.filter { it.level == 1 }.sortedBy { it.label }

        val cells: Map<Pair<Int, Long>, Float> = when (period) {
            Period.WEEK, Period.MONTH -> {
                points.associate { pt ->
                    val dayOffset = ChronoUnit.DAYS.between(startDate, LocalDate.parse(pt.localDay)).toInt()
                    val col = dayOffset.coerceIn(0, period.days - 1)
                    Pair(col, pt.primaryEmotionId) to pt.intensity
                }
            }
            Period.THREE_MONTHS -> buildMap {
                points.groupBy {
                    (ChronoUnit.DAYS.between(startDate, LocalDate.parse(it.localDay)) / 7).toInt()
                }.forEach { (weekIdx, weekPts) ->
                    weekPts.groupBy { it.primaryEmotionId }
                        .forEach { (emotionId, ePts) ->
                            put(Pair(weekIdx.coerceIn(0, 12), emotionId), ePts.maxOf { it.intensity })
                        }
                }
            }
        }

        val columnCount = when (period) { Period.WEEK -> 7; Period.MONTH -> 30; Period.THREE_MONTHS -> 13 }

        return if (cells.isEmpty()) HeatmapUiState.Empty(period)
        else HeatmapUiState.Ready(
            cells = cells,
            period = period,
            primaryEmotions = primaries,
            columnCount = columnCount,
            startDate = startDate,
            profileId = profileId,
            entryTypes = types
        )
    }

    private fun cellTimeRange(col: Int, startDate: LocalDate, period: Period): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        return when (period) {
            Period.WEEK, Period.MONTH -> {
                val day = startDate.plusDays(col.toLong())
                day.atStartOfDay(zone).toInstant().toEpochMilli() to
                    day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            }
            Period.THREE_MONTHS -> {
                val weekStart = startDate.plusDays(col * 7L)
                weekStart.atStartOfDay(zone).toInstant().toEpochMilli() to
                    weekStart.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
            }
        }
    }

    private fun buildColLabel(col: Int, startDate: LocalDate, period: Period): String =
        when (period) {
            Period.WEEK -> {
                val d = startDate.plusDays(col.toLong())
                "${arrayOf("Lun","Mar","Mer","Jeu","Ven","Sam","Dim")[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${monthFr(d.monthValue)}"
            }
            Period.MONTH -> {
                val d = startDate.plusDays(col.toLong())
                "${d.dayOfMonth} ${monthFr(d.monthValue)}"
            }
            Period.THREE_MONTHS -> {
                val start = startDate.plusDays(col * 7L)
                val end = start.plusDays(6)
                val monthPart = if (start.monthValue != end.monthValue)
                    "${monthFr(start.monthValue)}–${monthFr(end.monthValue)}"
                else monthFr(start.monthValue)
                "${start.dayOfMonth}–${end.dayOfMonth} $monthPart"
            }
        }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val entryRepository: EntryRepository,
        private val emotionRepository: EmotionRepository,
        private val partRepository: PartRepository,
        private val cycleDayRepository: CycleDayRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ExplorationViewModel(profileRepository, entryRepository, emotionRepository, partRepository, cycleDayRepository) as T
    }
}

internal fun monthFr(month: Int): String =
    arrayOf("jan","fév","mars","avr","mai","juin","juil","août","sep","oct","nov","déc")[month - 1]
