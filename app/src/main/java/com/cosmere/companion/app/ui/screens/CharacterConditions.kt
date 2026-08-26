package com.cosmere.companion.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cosmere.companion.core.data.RulesRepository
import com.cosmere.companion.core.model.PlayerCharacter

@Composable
internal fun ConditionsSection(
    character: PlayerCharacter,
    onUpdate: (PlayerCharacter) -> Unit,
    onOpenReference: (String) -> Unit,
) {
    val allConditions = remember { RulesRepository.conditions }
    var addMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Conditions", style = MaterialTheme.typography.titleMedium)
        Box {
            IconButton(onClick = { addMenuExpanded = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add condition")
            }
            val available = remember(character.activeConditions) {
                allConditions.filter { it.id !in character.activeConditions.keys }
            }
            DropdownMenu(expanded = addMenuExpanded, onDismissRequest = { addMenuExpanded = false }) {
                if (available.isEmpty()) {
                    DropdownMenuItem(text = { Text("All conditions active") }, onClick = {}, enabled = false)
                }
                available.forEach { condition ->
                    DropdownMenuItem(
                        text = { Text(condition.name) },
                        onClick = {
                            addMenuExpanded = false
                            onUpdate(character.copy(activeConditions = character.activeConditions + (condition.id to 1)))
                        },
                    )
                }
            }
        }
    }

    if (character.activeConditions.isEmpty()) {
        Text(
            "No active conditions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        val sorted = remember(character.activeConditions) {
            character.activeConditions.entries.sortedBy { (id, _) ->
                allConditions.firstOrNull { it.id == id }?.name ?: id
            }
        }
        sorted.forEach { (id, value) ->
            val condition = allConditions.firstOrNull { it.id == id }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SheetLink(condition?.name ?: id, onClick = { onOpenReference(conditionReferenceKey(id)) })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (condition?.hasBracketValue == true) {
                        IconButton(
                            onClick = {
                                onUpdate(
                                    character.copy(
                                        activeConditions = character.activeConditions + (id to (value - 1).coerceAtLeast(1)),
                                    ),
                                )
                            },
                            enabled = value > 1,
                        ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease ${condition.name} value") }
                        Text("$value", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                        IconButton(
                            onClick = {
                                onUpdate(character.copy(activeConditions = character.activeConditions + (id to value + 1)))
                            },
                        ) { Icon(Icons.Filled.Add, contentDescription = "Increase ${condition.name} value") }
                    }
                    IconButton(onClick = { onUpdate(character.copy(activeConditions = character.activeConditions - id)) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove ${condition?.name ?: id}")
                    }
                }
            }
        }
    }
}
