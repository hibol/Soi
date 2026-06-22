package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.entity.Emotion
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.data.entity.EntryType
import com.hibol.miette.soi.data.entity.Part
import com.hibol.miette.soi.data.entity.Tag
import com.hibol.miette.soi.data.repository.EmotionRepository
import com.hibol.miette.soi.data.repository.EntryRepository
import com.hibol.miette.soi.data.repository.PartRepository
import com.hibol.miette.soi.data.repository.ProfileRepository
import com.hibol.miette.soi.data.repository.TagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class SearchFilters(
    val type: EntryType? = null,
    val primaryEmotionId: Long? = null,
    val primaryEmotionLabel: String? = null,
    val tagLabel: String? = null,
    val partName: String? = null,
    val periodDays: Int? = null
) {
    val isEmpty: Boolean
        get() = type == null && primaryEmotionId == null && tagLabel == null
                && partName == null && periodDays == null
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    private val profileRepository: ProfileRepository,
    private val entryRepository: EntryRepository,
    private val emotionRepository: EmotionRepository,
    private val tagRepository: TagRepository,
    private val partRepository: PartRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    // Données pour les BottomSheets de filtre
    val primaryEmotions: StateFlow<List<Emotion>> = emotionRepository.getPrimaryEmotions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allParts: StateFlow<List<Part>> = profileRepository.getProfile()
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else partRepository.getAllByProfile(profile.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val debouncedQuery = _query.debounce(300)

    val results: StateFlow<List<Entry>> = combine(
        profileRepository.getProfile(),
        debouncedQuery,
        _filters
    ) { profile, query, filters ->
        Triple(profile, query, filters)
    }.flatMapLatest { (profile, query, filters) ->
        if (profile == null || (query.isBlank() && filters.isEmpty)) flowOf(emptyList())
        else entryRepository.search(
            profileId = profile.id,
            rawQuery = query,
            typeFilter = filters.type?.value,
            emotionId = filters.primaryEmotionId,
            tagLabel = filters.tagLabel,
            partName = filters.partName,
            periodDays = filters.periodDays
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(q: String) { _query.value = q }
    fun setFilter(filters: SearchFilters) { _filters.value = filters }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val entryRepository: EntryRepository,
        private val emotionRepository: EmotionRepository,
        private val tagRepository: TagRepository,
        private val partRepository: PartRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SearchViewModel(
                profileRepository, entryRepository,
                emotionRepository, tagRepository, partRepository
            ) as T
    }
}
