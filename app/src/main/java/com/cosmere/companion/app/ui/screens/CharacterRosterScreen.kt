package com.cosmere.companion.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cosmere.companion.app.data.readCharacterExport
import com.cosmere.companion.app.data.writeCharacterExport
import com.cosmere.companion.core.data.RulesRepository
import com.cosmere.companion.core.model.PlayerCharacter
import kotlinx.coroutines.launch

@Composable
internal fun CharacterRosterScreen(
    characters: List<PlayerCharacter>,
    onSelect: (PlayerCharacter) -> Unit,
    onCreateNew: () -> Unit,
    onDelete: (PlayerCharacter) -> Unit,
    onImport: (PlayerCharacter) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<PlayerCharacter?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            try {
                val imported = readCharacterExport(context, uri)
                onImport(imported)
                Toast.makeText(context, "Imported ${imported.name}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't read that file as a character export.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Characters", style = MaterialTheme.typography.headlineSmall)
            Row {
                IconButton(onClick = { importLauncher.launch("*/*") }) {
                    Icon(Icons.Filled.FileOpen, contentDescription = "Import character")
                }
                IconButton(onClick = onCreateNew) {
                    Icon(Icons.Filled.Add, contentDescription = "Create character")
                }
            }
        }

        if (characters.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "No characters yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onCreateNew) {
                    Text("Create Character")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(characters, key = { it.id }) { character ->
                    CharacterRosterRow(
                        character = character,
                        onClick = { onSelect(character) },
                        onDeleteRequest = { pendingDelete = character },
                    )
                }
            }
        }
    }

    pendingDelete?.let { character ->
        DeleteCharacterDialog(
            characterName = character.name,
            onConfirm = {
                onDelete(character)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun CharacterRosterRow(
    character: PlayerCharacter,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val ancestry = remember(character.ancestryId) { character.ancestryId?.let(RulesRepository::ancestryById) }
    val heroicPath = remember(character.heroicPathId) { RulesRepository.pathById(character.heroicPathId) }
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CharacterAvatar(avatarPath = character.avatarPath, name = character.name, size = 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(character.name, style = MaterialTheme.typography.titleMedium)
                val subtitle = buildString {
                    ancestry?.let { append(it.name) }
                    heroicPath?.let { path ->
                        if (isNotEmpty()) append(" · ")
                        append(path.name)
                        character.specialty?.let { append(" — $it") }
                    }
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                "Lv ${character.level} · ${character.currentHealth}/${character.maxHealth} HP",
                style = MaterialTheme.typography.bodySmall,
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options for ${character.name}")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Export") },
                        onClick = {
                            menuExpanded = false
                            coroutineScope.launch {
                                val uri = writeCharacterExport(context, character)
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export ${character.name}"))
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            onDeleteRequest()
                        },
                    )
                }
            }
        }
    }
}
