package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.data.entity.Part
import com.hibol.miette.soi.data.entity.PartTrait
import com.hibol.miette.soi.data.repository.EmotionRepository
import com.hibol.miette.soi.data.repository.EntryRepository
import com.hibol.miette.soi.data.repository.PartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PartReadViewModel(
    private val partRepository: PartRepository,
    private val entryRepository: EntryRepository,
    private val emotionRepository: EmotionRepository
) : ViewModel() {

    val part = MutableStateFlow<Part?>(null)
    val traits = MutableStateFlow<List<PartTrait>>(emptyList())
    val isDeleted = MutableStateFlow(false)
    val partEntries = MutableStateFlow<List<Entry>>(emptyList())
    val emotionColors = MutableStateFlow<Map<Long, MiniConstellationData>>(emptyMap())

    fun load(partId: Long) {
        viewModelScope.launch {
            partRepository.getById(partId).collect { part.value = it }
        }
        viewModelScope.launch {
            partRepository.getTraitsForPart(partId).collect { traits.value = it }
        }
        viewModelScope.launch {
            val initialPart = partRepository.getById(partId).filterNotNull().first()
            combine(
                partRepository.getExplicitEntriesForPart(partId),
                entryRepository.getAllEmotionsForProfile(initialPart.profileId),
                emotionRepository.getAllEmotions()
            ) { entries, allEntryEmotions, allEmotions ->
                partEntries.value = entries
                val entryIds = entries.map { it.id }.toSet()
                computeMiniConstellations(allEntryEmotions.filter { it.entryId in entryIds }, allEmotions)
            }.collect { colors ->
                emotionColors.value = colors
            }
        }
    }

    fun delete(partId: Long) {
        viewModelScope.launch {
            partRepository.delete(partId)
            isDeleted.value = true
        }
    }

    class Factory(
        private val partRepository: PartRepository,
        private val entryRepository: EntryRepository,
        private val emotionRepository: EmotionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PartReadViewModel(partRepository, entryRepository, emotionRepository) as T
    }
}
