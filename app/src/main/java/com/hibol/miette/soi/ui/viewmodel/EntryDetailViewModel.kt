package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.db.EntryPartRow
import com.hibol.miette.soi.data.entity.DreamEntry
import com.hibol.miette.soi.data.entity.Emotion
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.data.entity.Part
import com.hibol.miette.soi.data.entity.Tag
import com.hibol.miette.soi.data.repository.EmotionRepository
import com.hibol.miette.soi.data.repository.EntryRepository
import com.hibol.miette.soi.data.repository.PartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EntryDetailViewModel(
    private val entryRepository: EntryRepository,
    private val emotionRepository: EmotionRepository,
    private val partRepository: PartRepository
) : ViewModel() {

    val entry = MutableStateFlow<Entry?>(null)
    val emotions = MutableStateFlow<List<Pair<Emotion, Int>>>(emptyList())
    val primaryEmotions = MutableStateFlow<List<Emotion>>(emptyList())
    val tags = MutableStateFlow<List<Tag>>(emptyList())
    val dreamDetail = MutableStateFlow<DreamEntry?>(null)

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted

    private val _entryParts = MutableStateFlow<List<EntryPartRow>>(emptyList())
    val entryParts: StateFlow<List<EntryPartRow>> = _entryParts

    private val _allParts = MutableStateFlow<List<Part>>(emptyList())
    val allParts: StateFlow<List<Part>> = _allParts

    fun load(entryId: Long) {
        viewModelScope.launch {
            val e = entryRepository.getById(entryId).first() ?: return@launch
            entry.value = e

            // Émotions
            val entryEmotions = entryRepository.getEmotionsForEntry(entryId).first()
            val allEmotions = emotionRepository.getAllEmotions().first()
            emotions.value = entryEmotions.mapNotNull { entryEmotion ->
                allEmotions.find { it.id == entryEmotion.emotionId }
                    ?.let { Pair(it, entryEmotion.intensity) }
            }
            primaryEmotions.value = allEmotions.filter { it.level == 1 }

            // Tags
            tags.value = entryRepository.getTagsForEntry(entryId).first()

            // Détail rêve
            dreamDetail.value = entryRepository.getDreamDetail(entryId).first()

            // Parts — flux live (mis à jour à chaque confirm/remove)
            launch { partRepository.getPartsForEntry(entryId).collect { _entryParts.value = it } }
            launch { partRepository.getAllByProfile(e.profileId).collect { _allParts.value = it } }
        }
    }

    fun delete(entryId: Long) {
        viewModelScope.launch {
            entryRepository.delete(entryId)
            _isDeleted.value = true
        }
    }

    fun confirmPart(partId: Long) {
        val entryId = entry.value?.id ?: return
        viewModelScope.launch { partRepository.confirmEntryPart(entryId, partId) }
    }

    fun unconfirmPart(partId: Long) {
        val entryId = entry.value?.id ?: return
        viewModelScope.launch { partRepository.unconfirmEntryPart(entryId, partId) }
    }

    fun removePart(partId: Long) {
        val entryId = entry.value?.id ?: return
        viewModelScope.launch { partRepository.removeEntryPart(entryId, partId) }
    }

    fun linkPart(partId: Long) {
        val entryId = entry.value?.id ?: return
        viewModelScope.launch { partRepository.linkEntryPartExplicit(entryId, partId) }
    }

    fun redetect() {
        val e = entry.value ?: return
        viewModelScope.launch { partRepository.redetectParts(e.id, e.text ?: "", e.profileId) }
    }

    class Factory(
        private val entryRepository: EntryRepository,
        private val emotionRepository: EmotionRepository,
        private val partRepository: PartRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return EntryDetailViewModel(entryRepository, emotionRepository, partRepository) as T
        }
    }
}