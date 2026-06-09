package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.data.repository.EmotionRepository
import com.hibol.miette.soi.data.repository.EntryRepository
import com.hibol.miette.soi.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.YearMonth

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

    private val _emotionColors = MutableStateFlow<Map<Long, MiniConstellationData>>(emptyMap())
    val emotionColors: StateFlow<Map<Long, MiniConstellationData>> = _emotionColors

    private val _profileId = MutableStateFlow<Long?>(null)

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
        private val emotionRepository: EmotionRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(profileRepository, entryRepository, emotionRepository) as T
        }
    }
}