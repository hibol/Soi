package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.hibol.miette.soi.data.entity.Emotion
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.data.entity.EntryEmotion
import com.hibol.miette.soi.data.repository.EmotionRepository
import com.hibol.miette.soi.data.repository.EntryRepository
import com.hibol.miette.soi.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.YearMonth

data class EmotionBarData(
    val colorWeights: List<Pair<Color, Int>>,
    val intensityRatio: Float  // total intensité / (nb émotions × 5), dans [0, 1]
)

class HomeViewModel(
    private val profileRepository: ProfileRepository,
    private val entryRepository: EntryRepository,
    private val emotionRepository: EmotionRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries

    private val _profileName = MutableStateFlow("")
    val profileName: StateFlow<String> = _profileName

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    private val _emotionColors = MutableStateFlow<Map<Long, EmotionBarData>>(emptyMap())
    val emotionColors: StateFlow<Map<Long, EmotionBarData>> = _emotionColors

    private val _profileId = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            profileRepository.getProfile().collectLatest { profile ->
                profile?.let { p ->
                    _profileName.value = p.name
                    _profileId.value = p.id
                    val allEmotions = emotionRepository.getAllEmotions().first()
                    combine(
                        entryRepository.getAllByProfile(p.id),
                        entryRepository.getAllEmotionsForProfile(p.id)
                    ) { entries, entryEmotions ->
                        entries to computeEmotionColors(entryEmotions, allEmotions)
                    }.collectLatest { (entries, colors) ->
                        _entries.value = entries
                        _emotionColors.value = colors
                    }
                }
            }
        }
    }

    private fun computeEmotionColors(
        entryEmotions: List<EntryEmotion>,
        allEmotions: List<Emotion>
    ): Map<Long, EmotionBarData> = buildMap {
        entryEmotions.groupBy { it.entryId }.forEach { (entryId, emotions) ->
            val totalActual = emotions.sumOf { it.intensity }
            val maxPossible = emotions.size * 5
            val ratio = if (maxPossible > 0) totalActual.toFloat() / maxPossible else 0f

            val primaryIntensity = mutableMapOf<Long, Int>()
            emotions.forEach { ee ->
                val emotion = allEmotions.firstOrNull { it.id == ee.emotionId } ?: return@forEach
                val primaryId = if (emotion.level == 1) emotion.id else emotion.parentId ?: return@forEach
                primaryIntensity[primaryId] = (primaryIntensity[primaryId] ?: 0) + ee.intensity
            }
            if (primaryIntensity.isEmpty()) return@forEach

            val colorWeights = primaryIntensity
                .entries
                .sortedByDescending { it.value }
                .mapNotNull { (primaryId, intensity) ->
                    val hex = allEmotions.firstOrNull { it.id == primaryId }?.color ?: return@mapNotNull null
                    val color = try { Color(android.graphics.Color.parseColor(hex)) }
                    catch (_: IllegalArgumentException) { return@mapNotNull null }
                    color to intensity
                }
            if (colorWeights.isNotEmpty()) put(entryId, EmotionBarData(colorWeights, ratio))
        }
    }

    fun setMonth(month: YearMonth) {
        _currentMonth.value = month
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val entryRepository: EntryRepository,
        private val emotionRepository: EmotionRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(profileRepository, entryRepository, emotionRepository) as T
        }
    }
}