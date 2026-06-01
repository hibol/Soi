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

    val primaryEmotions = MutableStateFlow<List<Emotion>>(emptyList())
    val secondaryEmotions = MutableStateFlow<List<Emotion>>(emptyList())
    val allTags = tagRepository.getAllTags()

    private val _entrySaved = MutableStateFlow(false)
    val entrySaved: StateFlow<Boolean> = _entrySaved

    // Pour l'édition — état initial chargé depuis la DB
    private val _initialText = MutableStateFlow<String?>(null)
    val initialText: StateFlow<String?> = _initialText

    private val _initialEmotions = MutableStateFlow<List<EmotionSelection>>(emptyList())
    val initialEmotions: StateFlow<List<EmotionSelection>> = _initialEmotions

    private val _initialTags = MutableStateFlow<List<String>>(emptyList())
    val initialTags: StateFlow<List<String>> = _initialTags

    private val _initialMemoryQuality = MutableStateFlow("clair")
    val initialMemoryQuality: StateFlow<String> = _initialMemoryQuality

    private val _initialDate = MutableStateFlow<Long?>(null)
    val initialDate: StateFlow<Long?> = _initialDate

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

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

    fun loadEntry(entryId: Long) {
        viewModelScope.launch {
            val entry = entryRepository.getById(entryId).first() ?: return@launch
            _initialText.value = entry.text
            _initialDate.value = entry.entryDate

            // Charger les émotions liées
            val entryEmotions = entryRepository.getEmotionsForEntry(entryId).first()
            val allSecondary = secondaryEmotions.value
            _initialEmotions.value = entryEmotions.mapNotNull { entryEmotion ->
                allSecondary.firstOrNull { it.id == entryEmotion.emotionId }
                    ?.let { EmotionSelection(emotion = it, intensity = entryEmotion.intensity) }
            }

            // Charger les tags liés
            _initialTags.value = entryRepository.getTagsForEntry(entryId).first()
                .map { it.label }

            // Qualité du souvenir si rêve
            entryRepository.getDreamDetail(entryId).first()?.let {
                _initialMemoryQuality.value = it.memoryQuality
            }

            _isLoaded.value = true
        }
    }

    suspend fun saveDream(
        entryId: Long? = null,
        text: String?,
        memoryQuality: String,
        entryDate: Long,
        emotions: List<EmotionSelection>,
        tags: List<String>
    ) {
        val profileId = _profileId.value ?: return
        viewModelScope.launch {
            if (entryId == null) {
                entryRepository.createDreamEntry(
                    profileId = profileId,
                    entryDate = entryDate,
                    text = text,
                    memoryQuality = memoryQuality,
                    emotionIds = emotions.map { Pair(it.emotion.id, it.intensity) },
                    tagLabels = tags
                )
            } else {
                entryRepository.updateDreamEntry(
                    entryId = entryId,
                    text = text,
                    memoryQuality = memoryQuality,
                    entryDate = entryDate,
                    emotionIds = emotions.map { Pair(it.emotion.id, it.intensity) },
                    tagLabels = tags
                )
            }
            _entrySaved.value = true
        }
    }

    suspend fun saveSession(
        entryId: Long? = null,
        text: String?,
        entryDate: Long,
        emotions: List<EmotionSelection>,
        tags: List<String>
    ) {
        val profileId = _profileId.value ?: return
        viewModelScope.launch {
            if (entryId == null) {
                entryRepository.createSessionEntry(
                    profileId = profileId,
                    entryDate = entryDate,
                    text = text,
                    emotionIds = emotions.map { Pair(it.emotion.id, it.intensity) },
                    tagLabels = tags
                )
            } else {
                entryRepository.updateSessionEntry(
                    entryId = entryId,
                    text = text,
                    entryDate = entryDate,
                    emotionIds = emotions.map { Pair(it.emotion.id, it.intensity) },
                    tagLabels = tags
                )
            }
            _entrySaved.value = true
        }
    }

    suspend fun saveEvent(
        entryId: Long? = null,
        text: String?,
        entryDate: Long,
        emotions: List<EmotionSelection>,
        tags: List<String>
    ) {
        val profileId = _profileId.value ?: return
        viewModelScope.launch {
            if (entryId == null) {
                entryRepository.createEventEntry(
                    profileId = profileId,
                    entryDate = entryDate,
                    text = text,
                    emotionIds = emotions.map { Pair(it.emotion.id, it.intensity) },
                    tagLabels = tags
                )
            } else {
                entryRepository.updateEventEntry(
                    entryId = entryId,
                    text = text,
                    entryDate = entryDate,
                    emotionIds = emotions.map { Pair(it.emotion.id, it.intensity) },
                    tagLabels = tags
                )
            }
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