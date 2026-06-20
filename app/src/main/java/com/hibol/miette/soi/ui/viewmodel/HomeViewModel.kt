package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.data.repository.CycleDayRepository
import com.hibol.miette.soi.data.repository.EmotionRepository
import com.hibol.miette.soi.data.repository.EntryRepository
import com.hibol.miette.soi.data.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val profileRepository: ProfileRepository,
    private val entryRepository: EntryRepository,
    private val emotionRepository: EmotionRepository,
    private val cycleDayRepository: CycleDayRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries

    private val _profileName = MutableStateFlow("")
    val profileName: StateFlow<String> = _profileName

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    private val _emotionColors = MutableStateFlow<Map<Long, MiniConstellationData>>(emptyMap())
    val emotionColors: StateFlow<Map<Long, MiniConstellationData>> = _emotionColors

    private val _profileId = MutableStateFlow<Long?>(null)

    private val _cycleMode = MutableStateFlow(false)
    val cycleMode: StateFlow<Boolean> = _cycleMode.asStateFlow()

    val cycleDays: StateFlow<Set<LocalDate>> = combine(
        _currentMonth,
        profileRepository.getProfile()
    ) { month, profile -> month to profile }
        .flatMapLatest { (month, profile) ->
            if (profile == null) flowOf(emptySet())
            else cycleDayRepository.getForMonth(profile.id, month).map { it.toSet() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleCycleMode() { _cycleMode.value = !_cycleMode.value }

    fun toggleCycleDay(date: LocalDate) {
        val profileId = _profileId.value ?: return
        viewModelScope.launch { cycleDayRepository.toggle(profileId, date) }
    }

    init {
        viewModelScope.launch {
            profileRepository.getProfile().collectLatest { profile ->
                profile?.let { p ->
                    _profileName.value = p.name
                    _profileId.value = p.id
                    combine(
                        entryRepository.getAllByProfile(p.id),
                        entryRepository.getAllEmotionsForProfile(p.id),
                        emotionRepository.getAllEmotions()
                    ) { entries, entryEmotions, allEmotions ->
                        entries to computeMiniConstellations(entryEmotions, allEmotions)
                    }.collectLatest { (entries, colors) ->
                        _entries.value = entries
                        _emotionColors.value = colors
                    }
                }
            }
        }
    }

    fun setMonth(month: YearMonth) {
        _currentMonth.value = month
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val entryRepository: EntryRepository,
        private val emotionRepository: EmotionRepository,
        private val cycleDayRepository: CycleDayRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(profileRepository, entryRepository, emotionRepository, cycleDayRepository) as T
        }
    }
}