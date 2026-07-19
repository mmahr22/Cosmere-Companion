package com.cosmere.companion.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cosmere.companion.app.data.CompanionDatabase
import com.cosmere.companion.app.data.toDomain
import com.cosmere.companion.app.data.toEntity
import com.cosmere.companion.core.model.PlayerCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Holds the single active character sheet and persists it to Room so it
 * survives process death. There's no roster: creating a new character
 * (via [reset]) replaces whatever was saved before.
 */
class CharacterViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = CompanionDatabase.get(application).playerCharacterDao()

    private val _character = MutableStateFlow<PlayerCharacter?>(null)
    val character: StateFlow<PlayerCharacter?> = _character.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _character.value = dao.getCurrent()?.toDomain()
            _isLoading.value = false
        }
    }

    fun save(character: PlayerCharacter) {
        _character.value = character
        viewModelScope.launch { dao.upsert(character.toEntity()) }
    }

    fun reset() {
        _character.value = null
        viewModelScope.launch { dao.clear() }
    }
}
