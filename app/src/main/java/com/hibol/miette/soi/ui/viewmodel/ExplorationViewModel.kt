package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.entity.Emotion
import com.hibol.miette.soi.data.entity.EmotionTrendPoint
import com.hibol.miette.soi.data.entity.EntryType
import com.hibol.miette.soi.data.entity.TopSecondaryEmotion
import com.hibol.miette.soi.data.repository.EmotionRepository
import com.hibol.miette.soi.data.repository.EntryRepository
import com.hibol.miette.soi.data.repository.ProfileRepository
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
        val startEpochDay: Long,              // jour-0 de la période
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
    private val emotionRepository: EmotionRepository
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

    val heatmapUiState: StateFlow<HeatmapUiState> = combine(
        _selectedPeriod,
        _selectedEntryTypes,
        profileRepository.getProfile(),
        emotionRepository.getAllEmotions()
    ) { period, types, profile, emotions ->
        HeatmapInputs(period, types, profile?.id, emotions)
    }.flatMapLatest { inputs ->
        if (inputs.profileId == null) return@flatMapLatest flowOf(HeatmapUiState.Loading)
        val now = System.currentTimeMillis()
        val fromMillis = now - inputs.period.days.toLong() * 86_400_000L
        entryRepository.getHeatmapData(inputs.profileId, fromMillis, now, inputs.types.map { it.value })
            .map { points ->
                buildHeatmapState(points, inputs.allEmotions, inputs.period, fromMillis, inputs.profileId, inputs.types)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeatmapUiState.Loading)

    // Combine uniquement sur _selectedCell + heatmapUiState (déjà agrégé) — pas de 5e flow
    val tooltipUiState: StateFlow<TooltipUiState> = combine(
        _selectedCell,
        heatmapUiState
    ) { cell, heatmap -> cell to heatmap }
    .flatMapLatest { (cell, heatmap) ->
        if (cell == null || heatmap !is HeatmapUiState.Ready) {
            return@flatMapLatest flowOf(TooltipUiState.Hidden)
        }
        val (fromMillis, toMillis) = cellTimeRange(cell.col, heatmap.startEpochDay, heatmap.period)
        entryRepository.getTopSecondaryEmotions(
            heatmap.profileId, fromMillis, toMillis,
            heatmap.entryTypes.map { it.value }, cell.primaryEmotionId
        ).map { secondaries ->
            val primaryLabel = heatmap.primaryEmotions.find { it.id == cell.primaryEmotionId }?.label ?: ""
            if (secondaries.isEmpty()) TooltipUiState.Hidden
            else TooltipUiState.Visible(
                primaryLabel = primaryLabel,
                colLabel = buildColLabel(cell.col, heatmap.startEpochDay, heatmap.period),
                emotions = secondaries.map { e -> TooltipEmotion(e.label, e.color, e.maxIntensity) }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TooltipUiState.Hidden)

    private fun buildHeatmapState(
        points: List<EmotionTrendPoint>,
        allEmotions: List<Emotion>,
        period: Period,
        fromMillis: Long,
        profileId: Long,
        types: Set<EntryType>
    ): HeatmapUiState {
        val primaries = allEmotions.filter { it.level == 1 }.sortedBy { it.label }
        val startEpochDay = fromMillis / 86_400_000L

        val cells: Map<Pair<Int, Long>, Float> = when (period) {
            Period.WEEK, Period.MONTH -> {
                points.associate { pt ->
                    val col = (pt.dayEpoch - startEpochDay).toInt().coerceIn(0, period.days - 1)
                    Pair(col, pt.primaryEmotionId) to pt.intensity
                }
            }
            Period.THREE_MONTHS -> buildMap {
                // SQL donne MAX par jour — on prend le max de la semaine pour chaque émotion
                points.groupBy { ((it.dayEpoch - startEpochDay) / 7).toInt() }
                    .forEach { (weekIdx, weekPts) ->
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
            startEpochDay = startEpochDay,
            profileId = profileId,
            entryTypes = types
        )
    }

    private fun cellTimeRange(col: Int, startEpochDay: Long, period: Period): Pair<Long, Long> =
        when (period) {
            Period.WEEK, Period.MONTH -> {
                val s = (startEpochDay + col) * 86_400_000L
                s to s + 86_400_000L
            }
            Period.THREE_MONTHS -> {
                val s = (startEpochDay + col * 7L) * 86_400_000L
                s to s + 7L * 86_400_000L
            }
        }

    private fun buildColLabel(col: Int, startEpochDay: Long, period: Period): String =
        when (period) {
            Period.WEEK -> {
                val d = LocalDate.ofEpochDay(startEpochDay + col)
                "${arrayOf("Lun","Mar","Mer","Jeu","Ven","Sam","Dim")[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${monthFr(d.monthValue)}"
            }
            Period.MONTH -> {
                val d = LocalDate.ofEpochDay(startEpochDay + col)
                "${d.dayOfMonth} ${monthFr(d.monthValue)}"
            }
            Period.THREE_MONTHS -> {
                val start = LocalDate.ofEpochDay(startEpochDay + col * 7L)
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
        private val emotionRepository: EmotionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ExplorationViewModel(profileRepository, entryRepository, emotionRepository) as T
    }
}

internal fun monthFr(month: Int): String =
    arrayOf("jan","fév","mars","avr","mai","juin","juil","août","sep","oct","nov","déc")[month - 1]
