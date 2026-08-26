package com.cosmere.companion.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Top-level router for the Characters tab: a loading spinner while Room's initial query is in
 * flight, then one of the roster, the creation wizard, or an open character's sheet, depending on
 * [openCharacterId]/[isCreating]. The actual screens live in their own files —
 * [CharacterRosterScreen], [CharacterCreationForm], [CharacterSheet] — split out of what used to
 * be one large file covering the whole Characters tab.
 */
@Composable
fun CharactersScreen(
    onOpenReference: (String) -> Unit = {},
    viewModel: CharacterViewModel = viewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()

    // 0 means "no character open" — real ids start at 1 (Room autoGenerate).
    var openCharacterId by rememberSaveable { mutableStateOf(0) }
    var isCreating by rememberSaveable { mutableStateOf(false) }
    val openCharacter = characters.firstOrNull { it.id == openCharacterId }

    when {
        isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        isCreating -> CharacterCreationForm(
            onCreate = { character ->
                viewModel.save(character)
                isCreating = false
            },
            onCancel = { isCreating = false },
        )
        openCharacter != null -> CharacterSheet(
            character = openCharacter,
            onUpdate = viewModel::save,
            onBack = { openCharacterId = 0 },
            onDelete = {
                viewModel.delete(openCharacter)
                openCharacterId = 0
            },
            onOpenReference = onOpenReference,
        )
        else -> CharacterRosterScreen(
            characters = characters,
            onSelect = { openCharacterId = it.id },
            onCreateNew = { isCreating = true },
            onDelete = viewModel::delete,
            onImport = viewModel::save,
        )
    }
}
