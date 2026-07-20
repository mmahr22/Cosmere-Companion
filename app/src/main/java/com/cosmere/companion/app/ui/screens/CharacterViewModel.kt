package com.cosmere.companion.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cosmere.companion.app.data.CompanionDatabase
import com.cosmere.companion.app.data.deleteAvatar
import com.cosmere.companion.app.data.toDomain
import com.cosmere.companion.app.data.toEntity
import com.cosmere.companion.core.model.PlayerCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds the character roster and persists it to Room. [characters] is
 * sourced directly from Room's Flow, so every [save]/[delete] is reflected
 * back through the same reactive list rather than tracked separately.
 */
class CharacterViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = CompanionDatabase.get(application).playerCharacterDao()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val characters: StateFlow<List<PlayerCharacter>> = dao.getAll()
        .onEach { _isLoading.value = false }
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun save(character: PlayerCharacter) {
        viewModelScope.launch { dao.upsert(character.toEntity()) }
    }

    fun delete(character: PlayerCharacter) {
        viewModelScope.launch { dao.delete(character.id) }
        deleteAvatar(character.avatarPath)
    }
}
