package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.entity.DreamEntry
import com.hibol.miette.soi.data.entity.Emotion
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.data.entity.Tag
import com.hibol.miette.soi.data.repository.EmotionRepository
import com.hibol.miette.soi.data.repository.EntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EntryDetailViewModel(
    private val entryRepository: EntryRepository,
    private val emotionRepository: EmotionRepository
) : ViewModel() {

    val entry = MutableStateFlow<Entry?>(null)
    val emotions = MutableStateFlow<List<Pair<Emotion, Int>>>(emptyList())
    val tags = MutableStateFlow<List<Tag>>(emptyList())
    val dreamDetail = MutableStateFlow<DreamEntry?>(null)

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted

    fun load(entryId: Long) {
        viewModelScope.launch {
            entry.value = entryRepository.getById(entryId).first()

            // Charger les émotions avec leur label
            val entryEmotions = entryRepository.getEmotionsForEntry(entryId).first()
            val allEmotions = emotionRepository.getAllEmotions().first()
            emotions.value = entryEmotions.mapNotNull { entryEmotion ->
                allEmotions.find { it.id == entryEmotion.emotionId }
                    ?.let { Pair(it, entryEmotion.intensity) }
            }

            // Tags
            tags.value = entryRepository.getTagsForEntry(entryId).first()

            // Détail rêve
            dreamDetail.value = entryRepository.getDreamDetail(entryId).first()
        }
    }

    fun delete(entryId: Long) {
        viewModelScope.launch {
            entryRepository.delete(entryId)
            _isDeleted.value = true
        }
    }

    class Factory(
        private val entryRepository: EntryRepository,
        private val emotionRepository: EmotionRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return EntryDetailViewModel(entryRepository, emotionRepository) as T
        }
    }
}