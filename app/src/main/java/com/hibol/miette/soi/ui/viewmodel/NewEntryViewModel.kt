package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.entity.Emotion
import com.hibol.miette.soi.data.repository.EmotionRepository
import com.hibol.miette.soi.data.repository.EntryRepository
import com.hibol.miette.soi.data.repository.ProfileRepository
import com.hibol.miette.soi.data.repository.TagRepository
import com.hibol.miette.soi.ui.components.EmotionSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NewEntryViewModel(
    private val profileRepository: ProfileRepository,
    private val entryRepository: EntryRepository,
    private val emotionRepository: EmotionRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _profileId = MutableStateFlow<Long?>(null)

    val primaryEmotions: MutableStateFlow<List<Emotion>> = MutableStateFlow(emptyList())
    val secondaryEmotions: MutableStateFlow<List<Emotion>> = MutableStateFlow(emptyList())
    val allTags = tagRepository.getAllTags()

    private val _entrySaved = MutableStateFlow(false)
    val entrySaved: StateFlow<Boolean> = _entrySaved

    init {
        viewModelScope.launch {
            profileRepository.getProfile().collectLatest { profile ->
                profile?.let { _profileId.value = it.id }
            }
        }
        viewModelScope.launch {
            primaryEmotions.value = emotionRepository.getPrimaryEmotions().first()
            secondaryEmotions.value = emotionRepository.getAllEmotions()
                .first().filter { it.level == 2 }
        }
    }

    suspend fun saveDream(
        text: String?,
        memoryQuality: String,
        entryDate: Long,
        emotions: List<EmotionSelection>,
        tags: List<String>
    ) {
        val profileId = _profileId.value ?: return
        viewModelScope.launch {
            entryRepository.createDreamEntry(
                profileId = profileId,
                entryDate = entryDate,
                text = text,
                memoryQuality = memoryQuality,
                emotionIds = emotions.map { Pair(it.emotion.id, it.intensity) },
                tagLabels = tags
            )
            _entrySaved.value = true
        }
    }

    suspend fun saveSession(
        text: String?,
        entryDate: Long,
        emotions: List<EmotionSelection>,
        tags: List<String>
    ) {
        val profileId = _profileId.value ?: return
        viewModelScope.launch {
            entryRepository.createSessionEntry(
                profileId = profileId,
                entryDate = entryDate,
                text = text,
                emotionIds = emotions.map { Pair(it.emotion.id, it.intensity) },
                tagLabels = tags
            )
            _entrySaved.value = true
        }
    }

    suspend fun saveEvent(
        text: String?,
        entryDate: Long,
        emotions: List<EmotionSelection>,
        tags: List<String>
    ) {
        val profileId = _profileId.value ?: return
        viewModelScope.launch {
            entryRepository.createEventEntry(
                profileId = profileId,
                entryDate = entryDate,
                text = text,
                emotionIds = emotions.map { Pair(it.emotion.id, it.intensity) },
                tagLabels = tags
            )
            _entrySaved.value = true
        }
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val entryRepository: EntryRepository,
        private val emotionRepository: EmotionRepository,
        private val tagRepository: TagRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return NewEntryViewModel(
                profileRepository,
                entryRepository,
                emotionRepository,
                tagRepository
            ) as T
        }
    }
}