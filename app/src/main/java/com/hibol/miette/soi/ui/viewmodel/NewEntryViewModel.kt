package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.entity.Emotion
import com.hibol.miette.soi.data.repository.EmotionRepository
import com.hibol.miette.soi.data.repository.EntryRepository
import com.hibol.miette.soi.data.repository.PartRepository
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
    private val tagRepository: TagRepository,
    private val partRepository: PartRepository
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

    private val _initialIsBlurred = MutableStateFlow(false)
    val initialIsBlurred: StateFlow<Boolean> = _initialIsBlurred

    private val _initialOrientation = MutableStateFlow<Float?>(null)
    val initialOrientation: StateFlow<Float?> = _initialOrientation

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
            // On interroge le repository directement plutôt que de lire secondaryEmotions.value,
            // qui peut encore être vide si le init n'a pas fini de charger (race condition).
            val entryEmotions = entryRepository.getEmotionsForEntry(entryId).first()
            val allEmotions = emotionRepository.getAllEmotions().first()
            _initialEmotions.value = entryEmotions.mapNotNull { entryEmotion ->
                allEmotions.firstOrNull { it.id == entryEmotion.emotionId }
                    ?.let { EmotionSelection(emotion = it, intensity = entryEmotion.intensity) }
            }

            // Charger les tags liés
            _initialTags.value = entryRepository.getTagsForEntry(entryId).first()
                .map { it.label }

            _initialIsBlurred.value = entry.isBlurred != 0
            _initialOrientation.value = entry.orientation

            // Qualité du souvenir si rêve
            entryRepository.getDreamDetail(entryId).first()?.let {
                _initialMemoryQuality.value = it.memoryQuality
            }

            _isLoaded.value = true
        }
    }

    fun saveDream(
        entryId: Long? = null,
        text: String?,
        memoryQuality: String,
        entryDate: Long,
        emotions: List<EmotionSelection>,
        tags: List<String>,
        isBlurred: Boolean = false,
        orientation: Float? = null
    ) = saveEntry(text) { profileId ->
        entryId?.let { id ->
            entryRepository.updateDreamEntry(
                entryId = id,
                text = text,
                memoryQuality = memoryQuality,
                entryDate = entryDate,
                emotionIds = emotions.map { Pair(it.emotion.id, it.intensity) },
                tagLabels = tags,
                isBlurred = isBlurred,
                orientation = orientation
            )
            id
        } ?: entryRepository.createDreamEntry(
            profileId = profileId,
            entryDate = entryDate,
            text = text,
            memoryQuality = memoryQuality,
            emotionIds = emotions.map { Pair(it.emotion.id, it.intensity) },
            tagLabels = tags,
            isBlurred = isBlurred,
            orientation = orientation
        )
    }

    fun saveSession(
        entryId: Long? = null,
        text: String?,
        entryDate: Long,
        emotions: List<EmotionSelection>,
        tags: List<String>,
        isBlurred: Boolean = false,
        orientation: Float? = null
    ) = saveEntry(text) { profileId ->
        entryId?.let { id ->
            entryRepository.updateSessionEntry(
                entryId = id,
                text = text,
                entryDate = entryDate,
                emotionIds = emotions.map { Pair(it.emotion.id, it.intensity) },
                tagLabels = tags,
                isBlurred = isBlurred,
                orientation = orientation
            )
            id
        } ?: entryRepository.createSessionEntry(
            profileId = profileId,
            entryDate = entryDate,
            text = text,
            emotionIds = emotions.map { Pair(it.emotion.id, it.intensity) },
            tagLabels = tags,
            isBlurred = isBlurred,
            orientation = orientation
        )
    }

    fun saveEvent(
        entryId: Long? = null,
        text: String?,
        entryDate: Long,
        emotions: List<EmotionSelection>,
        tags: List<String>,
        isBlurred: Boolean = false,
        orientation: Float? = null
    ) = saveEntry(text) { profileId ->
        entryId?.let { id ->
            entryRepository.updateEventEntry(
                entryId = id,
                text = text,
                entryDate = entryDate,
                emotionIds = emotions.map { Pair(it.emotion.id, it.intensity) },
                tagLabels = tags,
                isBlurred = isBlurred,
                orientation = orientation
            )
            id
        } ?: entryRepository.createEventEntry(
            profileId = profileId,
            entryDate = entryDate,
            text = text,
            emotionIds = emotions.map { Pair(it.emotion.id, it.intensity) },
            tagLabels = tags,
            isBlurred = isBlurred,
            orientation = orientation
        )
    }

    // Lance la détection en arrière-plan sans bloquer la navigation
    private fun saveEntry(text: String?, block: suspend (profileId: Long) -> Long) {
        val profileId = _profileId.value ?: return
        viewModelScope.launch {
            val savedId = block(profileId)
            launch { partRepository.detectAndLinkParts(savedId, text ?: "", profileId) }
            _entrySaved.value = true
        }
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val entryRepository: EntryRepository,
        private val emotionRepository: EmotionRepository,
        private val tagRepository: TagRepository,
        private val partRepository: PartRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return NewEntryViewModel(
                profileRepository,
                entryRepository,
                emotionRepository,
                tagRepository,
                partRepository
            ) as T
        }
    }
}