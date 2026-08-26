package com.cosmere.companion.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cosmere.companion.core.data.RulesRepository
import com.cosmere.companion.core.model.Attribute
import com.cosmere.companion.core.model.PlayerCharacter

@Composable
internal fun SingerFormsSection(character: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    val allForms = remember { RulesRepository.singerForms }
    val currentForm = remember(character.currentFormId) {
        character.currentFormId?.let(RulesRepository::singerFormById)
    }

    Text("Forms", style = MaterialTheme.typography.titleMedium)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (currentForm != null) {
                Text(
                    "Current form: ${currentForm.name}" + if (currentForm.voidform) " (Voidspren)" else "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text("Bonded to ${currentForm.sprenBond}", style = MaterialTheme.typography.bodySmall)
                if (currentForm.attributeBonuses.isNotEmpty()) {
                    Text(
                        currentForm.attributeBonuses.entries.joinToString(", ") { (attrName, bonus) ->
                            val abbreviation = Attribute.entries.find { it.name == attrName }?.abbreviation ?: attrName
                            "$abbreviation +$bonus"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(currentForm.summary, style = MaterialTheme.typography.bodySmall)
            } else {
                Text("No form active", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    Text("Change form", style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        character.availableFormIds.mapNotNull(RulesRepository::singerFormById).forEach { form ->
            FilterChip(
                selected = character.currentFormId == form.id,
                onClick = {
                    val newCurrent = if (character.currentFormId == form.id) null else form.id
                    onUpdate(character.copy(currentFormId = newCurrent))
                },
                label = { Text(form.name) },
            )
        }
    }

    Text("Unlocked from Singer talents", style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        allForms.filter { it.grantedByTalentId != null }.forEach { form ->
            val unlocked = form.id in character.unlockedFormIds
            FilterChip(
                selected = unlocked,
                onClick = {
                    val newUnlocked = if (unlocked) character.unlockedFormIds - form.id else character.unlockedFormIds + form.id
                    val newCurrent = if (unlocked && character.currentFormId == form.id) null else character.currentFormId
                    onUpdate(character.copy(unlockedFormIds = newUnlocked, currentFormId = newCurrent))
                },
                label = { Text(form.name) },
            )
        }
    }
}
