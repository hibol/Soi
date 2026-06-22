package com.hibol.miette.soi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.data.repository.EntryRepository
import com.hibol.miette.soi.data.repository.ProfileRepository
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

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    private val profileRepository: ProfileRepository,
    private val entryRepository: EntryRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<List<Entry>> = combine(
        profileRepository.getProfile(),
        _query.debounce(300)
    ) { profile, query ->
        profile to query
    }.flatMapLatest { (profile, query) ->
        if (profile == null || query.isBlank()) flowOf(emptyList())
        else entryRepository.search(profile.id, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(q: String) {
        _query.value = q
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val entryRepository: EntryRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(profileRepository, entryRepository) as T
        }
    }
}
