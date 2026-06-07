package com.hibol.miette.soi.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hibol.miette.soi.data.entity.Part
import com.hibol.miette.soi.data.entity.PartTrait
import com.hibol.miette.soi.data.repository.PartRepository
import com.hibol.miette.soi.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PartDetailViewModel(
    private val partRepository: PartRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    // Champs du formulaire (observables par Compose via mutableStateOf)
    var name by mutableStateOf("")
    var age by mutableStateOf("")
    var description by mutableStateOf("")
    var role by mutableStateOf<String?>(null)

    private val _selectedTraits = MutableStateFlow<List<PartTrait>>(emptyList())
    val selectedTraits: StateFlow<List<PartTrait>> = _selectedTraits

    // Flow Room : se met à jour automatiquement si le seeding n'est pas encore terminé au démarrage
    val allPresetTraits: StateFlow<List<PartTrait>> = partRepository.observePresetTraits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading = MutableStateFlow(true)
    val savedPartId = MutableStateFlow<Long?>(null)
    val saveError = MutableStateFlow<String?>(null)

    private var currentPartId: Long? = null
    private var profileId: Long = 0L

    fun load(partId: Long?, initialRole: String? = null) {
        viewModelScope.launch {
            profileId = profileRepository.getProfile().filterNotNull().first().id

            if (partId != null) {
                currentPartId = partId
                partRepository.getById(partId).filterNotNull().first().let { part ->
                    name = part.name
                    age = part.age ?: ""
                    description = part.description ?: ""
                    role = part.role
                }
                _selectedTraits.value = partRepository.getTraitsForPartOnce(partId)
            } else {
                role = initialRole
            }
            isLoading.value = false
        }
    }

    fun addTrait(trait: PartTrait) {
        if (_selectedTraits.value.none { it.id == trait.id }) {
            _selectedTraits.update { it + trait }
        }
    }

    fun addTraitByLabel(label: String) {
        viewModelScope.launch {
            val trait = partRepository.getOrCreateManualTrait(label)
            addTrait(trait)
        }
    }

    fun removeTrait(trait: PartTrait) {
        _selectedTraits.update { it.filter { t -> t.id != trait.id } }
    }

    fun save() {
        viewModelScope.launch {
            if (name.isBlank()) {
                saveError.value = "Le nom est requis"
                return@launch
            }
            saveError.value = null
            val now = System.currentTimeMillis()
            val id = currentPartId

            val savedId: Long = if (id == null) {
                partRepository.create(
                    Part(
                        profileId = profileId,
                        name = name.trim(),
                        age = age.trim().ifEmpty { null },
                        description = description.trim().ifEmpty { null },
                        role = role,
                        createdAt = now,
                        updatedAt = now
                    )
                ).also { currentPartId = it }
            } else {
                partRepository.getById(id).filterNotNull().first().let { existing ->
                    partRepository.update(
                        existing.copy(
                            name = name.trim(),
                            age = age.trim().ifEmpty { null },
                            description = description.trim().ifEmpty { null },
                            role = role,
                            updatedAt = now
                        )
                    )
                }
                id
            }

            partRepository.syncTraits(savedId, _selectedTraits.value.map { it.id }.toSet())
            savedPartId.value = savedId
        }
    }

    class Factory(
        private val partRepository: PartRepository,
        private val profileRepository: ProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PartDetailViewModel(partRepository, profileRepository) as T
    }
}
