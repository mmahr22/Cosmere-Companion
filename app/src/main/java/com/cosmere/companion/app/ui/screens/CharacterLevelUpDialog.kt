package com.cosmere.companion.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cosmere.companion.core.data.RulesRepository
import com.cosmere.companion.core.model.Attribute
import com.cosmere.companion.core.model.CharacterMath
import com.cosmere.companion.core.model.GamePath
import com.cosmere.companion.core.model.PlayerCharacter
import com.cosmere.companion.core.model.Skill

/**
 * The only place attributes, skills, surges, and talents can be spent — reached by tapping
 * "Level {N}" or the "Level Up" button on the sheet header. Keeping allocation here (instead of
 * editable inline on the Main/Skills/Talents tabs) makes those tabs a stable read-only reference
 * during play; GM-granted bonus points (spent here too) are set separately on the GM tab.
 */
@Composable
internal fun LevelUpDialog(
    character: PlayerCharacter,
    heroicPath: GamePath?,
    radiantPath: GamePath?,
    onUpdate: (PlayerCharacter) -> Unit,
    onOpenReference: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    fun updateSkillRank(skillId: String, newRank: Int) {
        val clamped = newRank.coerceIn(0, CharacterMath.maxSkillRank(character.level))
        onUpdate(character.copy(skillRanks = character.skillRanks + (skillId to clamped)))
    }

    val skillPointsSpent = Skill.entries.sumOf { skill ->
        val autoMinimum = if (skill.name == heroicPath?.startingSkillId) 1 else 0
        (character.skillRank(skill.name) - autoMinimum).coerceAtLeast(0)
    }
    val skillPointsRemaining = (character.totalSkillRanks - 1) - skillPointsSpent
    val attributePointsRemaining = character.totalAttributePoints - character.attributes.values.sum()
    val skillCap = CharacterMath.maxSkillRank(character.level)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Level ${character.level}", style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Done")
                    }
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Attributes" + pointsSuffix(attributePointsRemaining),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Attribute.entries.forEach { attribute ->
                        val value = character.attribute(attribute)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("${attribute.displayName} (${attribute.abbreviation})")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (value > 0) {
                                            onUpdate(character.copy(attributes = character.attributes + (attribute to value - 1)))
                                        }
                                    },
                                    enabled = value > 0,
                                ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease ${attribute.displayName}") }
                                Text("$value", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                IconButton(
                                    onClick = {
                                        if (value < CharacterMath.ATTRIBUTE_LEVEL_CAP && attributePointsRemaining > 0) {
                                            onUpdate(character.copy(attributes = character.attributes + (attribute to value + 1)))
                                        }
                                    },
                                    enabled = value < CharacterMath.ATTRIBUTE_LEVEL_CAP && attributePointsRemaining > 0,
                                ) { Icon(Icons.Filled.Add, contentDescription = "Increase ${attribute.displayName}") }
                            }
                        }
                    }

                    HorizontalDivider()

                    Text(
                        "Skills" + pointsSuffix(skillPointsRemaining),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Attribute.entries.forEach { attribute ->
                        Text(attribute.displayName, style = MaterialTheme.typography.labelLarge)
                        Skill.forAttribute(attribute).forEach { skill ->
                            val autoMinimum = if (skill.name == heroicPath?.startingSkillId) 1 else 0
                            val rank = character.skillRank(skill.name)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(skill.displayName + if (autoMinimum > 0) " (path)" else "")
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (rank > autoMinimum) updateSkillRank(skill.name, rank - 1) },
                                        enabled = rank > autoMinimum,
                                    ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease ${skill.displayName}") }
                                    Text("$rank", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                    IconButton(
                                        onClick = {
                                            if (rank < skillCap && skillPointsRemaining > 0) updateSkillRank(skill.name, rank + 1)
                                        },
                                        enabled = rank < skillCap && skillPointsRemaining > 0,
                                    ) { Icon(Icons.Filled.Add, contentDescription = "Increase ${skill.displayName}") }
                                }
                            }
                        }
                    }

                    val surgeIds = character.skillRanks.keys.filter { key -> Skill.entries.none { it.name == key } }
                    if (surgeIds.isNotEmpty()) {
                        Text("Surges", style = MaterialTheme.typography.labelLarge)
                        surgeIds.forEach { surgeId ->
                            val rank = character.skillRank(surgeId)
                            val surgeName = RulesRepository.surgeById(surgeId)?.name ?: surgeId
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(surgeName)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { updateSkillRank(surgeId, rank - 1) },
                                        enabled = rank > 0,
                                    ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease $surgeName") }
                                    Text("$rank", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                    IconButton(
                                        onClick = { updateSkillRank(surgeId, rank + 1) },
                                        enabled = rank < skillCap,
                                    ) { Icon(Icons.Filled.Add, contentDescription = "Increase $surgeName") }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    TalentsSection(
                        character = character,
                        heroicPath = heroicPath,
                        radiantPath = radiantPath,
                        onUpdate = onUpdate,
                        onOpenReference = onOpenReference,
                    )
                }
            }
        }
    }
}
