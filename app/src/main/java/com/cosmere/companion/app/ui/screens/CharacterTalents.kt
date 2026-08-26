package com.cosmere.companion.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cosmere.companion.core.data.RulesRepository
import com.cosmere.companion.core.model.Activation
import com.cosmere.companion.core.model.GamePath
import com.cosmere.companion.core.model.PlayerCharacter
import com.cosmere.companion.core.model.Talent

/**
 * The Talents budget/purchase UI, reached from [LevelUpDialog]. Buying a talent that's a
 * heroic/Radiant path's key talent opens that whole path — see
 * [com.cosmere.companion.core.model.PlayerCharacter.accessiblePathIds].
 */
@Composable
internal fun TalentsSection(
    character: PlayerCharacter,
    heroicPath: GamePath?,
    radiantPath: GamePath?,
    onUpdate: (PlayerCharacter) -> Unit,
    onOpenReference: (String) -> Unit,
) {
    val ancestry = remember(character.ancestryId) { character.ancestryId?.let { RulesRepository.ancestryById(it) } }
    val freeTalentIds = remember(heroicPath, radiantPath, ancestry) {
        listOfNotNull(heroicPath?.keyTalentId, radiantPath?.keyTalentId, ancestry?.keyTalentId)
    }
    val spent = character.purchasedTalentIds.count { it !in freeTalentIds }
    val remaining = character.totalTalentPoints - spent

    // Paths already open to this character (granted at creation, or unlocked since by
    // purchasing another path's key talent — see PlayerCharacter.accessiblePathIds).
    val accessiblePathIds = remember(character.purchasedTalentIds, character.heroicPathId, character.radiantPathId, character.ancestryId) {
        character.accessiblePathIds
    }
    val purchasedTalents = remember(character.purchasedTalentIds) {
        character.purchasedTalentIds.mapNotNull { RulesRepository.talentById(it) }.sortedBy { it.name }
    }
    // Talents from already-open trees, plus the key talent of any heroic path not yet taken —
    // per the book, "you can follow as many heroic paths and specialties as you wish." — plus,
    // if not yet Radiant, each order's First Ideal talent (swearing it is how a character bonds
    // a spren and becomes Radiant through play, not just at creation); its own prerequisiteLevel
    // already gates this to level 2+.
    val available = remember(character.purchasedTalentIds, accessiblePathIds, character.radiantPathId, character.skillRanks, character.level) {
        val newHeroicPathKeyTalents = RulesRepository.paths
            .filter { it.type == "heroic" && it.id !in accessiblePathIds }
            .mapNotNull { RulesRepository.talentById(it.keyTalentId) }
        val newRadiantPathKeyTalents = if (character.radiantPathId == null) {
            RulesRepository.paths.filter { it.type == "radiant" }.mapNotNull { RulesRepository.talentById(it.keyTalentId) }
        } else {
            emptyList()
        }
        (accessiblePathIds.flatMap { RulesRepository.talentsForPath(it) } + newHeroicPathKeyTalents + newRadiantPathKeyTalents)
            .distinctBy { it.id }
            .filter { it.id !in character.purchasedTalentIds && talentPrerequisitesMet(it, character) }
            .sortedBy { it.name }
    }
    var addMenuExpanded by remember { mutableStateOf(false) }
    var detailTalentId by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Talents" + pointsSuffix(remaining), style = MaterialTheme.typography.titleMedium)
        Box {
            IconButton(onClick = { addMenuExpanded = true }, enabled = remaining > 0) {
                Icon(Icons.Filled.Add, contentDescription = "Add talent")
            }
            DropdownMenu(expanded = addMenuExpanded, onDismissRequest = { addMenuExpanded = false }) {
                if (available.isEmpty()) {
                    DropdownMenuItem(text = { Text("No eligible talents") }, onClick = {}, enabled = false)
                }
                available.forEach { talent ->
                    val isNewPath = talent.isKey && talent.pathId !in accessiblePathIds
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(talent.name)
                                val talentPath = RulesRepository.pathById(talent.pathId)
                                val subtitle = if (isNewPath) {
                                    val label = if (talentPath?.type == "radiant") "New Radiant order" else "New path"
                                    "$label — ${talentPath?.name ?: talent.pathId}"
                                } else {
                                    talent.specialty ?: talentPath?.name
                                }
                                subtitle?.let {
                                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        onClick = {
                            addMenuExpanded = false
                            val talentPath = RulesRepository.pathById(talent.pathId)
                            if (talent.isKey && talentPath?.type == "radiant") {
                                // Swearing the First Ideal bonds a spren: adopt the order and grant its two surges,
                                // mirroring what the creation wizard does for a character who starts already Radiant.
                                var skillRanks = character.skillRanks
                                talentPath.surgeIds.forEach { surgeId ->
                                    skillRanks = skillRanks + (surgeId to maxOf(skillRanks[surgeId] ?: 0, 1))
                                }
                                onUpdate(
                                    character.copy(
                                        purchasedTalentIds = character.purchasedTalentIds + talent.id,
                                        radiantPathId = talentPath.id,
                                        skillRanks = skillRanks,
                                    ),
                                )
                            } else {
                                onUpdate(character.copy(purchasedTalentIds = character.purchasedTalentIds + talent.id))
                            }
                        },
                    )
                }
            }
        }
    }

    if (purchasedTalents.isEmpty()) {
        Text(
            "No talents yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        purchasedTalents.forEach { talent ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SheetLink(talent.name, onClick = { detailTalentId = talent.id })
                    Text(
                        talent.specialty ?: (RulesRepository.pathById(talent.pathId)?.name ?: talent.pathId),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (talent.id !in freeTalentIds) {
                    IconButton(
                        onClick = {
                            onUpdate(character.copy(purchasedTalentIds = character.purchasedTalentIds - talent.id))
                        },
                    ) { Icon(Icons.Filled.Close, contentDescription = "Remove ${talent.name}") }
                }
            }
        }
    }

    detailTalentId?.let { id ->
        TalentDetailDialog(talentId = id, onDismiss = { detailTalentId = null })
    }
}

/** Checks the prerequisites the app can verify automatically; [Talent.prerequisiteOther] is left to the player/GM to judge. */
private fun talentPrerequisitesMet(talent: Talent, character: PlayerCharacter): Boolean {
    val talentsOk = when (talent.prerequisiteTalentsMode) {
        "any" -> talent.prerequisiteTalents.isEmpty() || talent.prerequisiteTalents.any { it in character.purchasedTalentIds }
        else -> talent.prerequisiteTalents.all { it in character.purchasedTalentIds }
    }
    val skillsOk = talent.prerequisiteSkills.all { (skillId, rank) -> character.skillRank(skillId) >= rank }
    val levelOk = talent.prerequisiteLevel?.let { character.level >= it } ?: true
    val idealOk = talent.prerequisiteIdealSpoken?.let { character.spokenIdeal >= it } ?: true
    return talentsOk && skillsOk && levelOk && idealOk
}

/**
 * A talent's full rules text as an in-place popup, so reviewing a talent from the character
 * sheet doesn't leave the sheet for the Reference tab. Tapping a prerequisite talent swaps the
 * popup to that talent instead of leaving the sheet either; dismiss by tapping outside it.
 */
@Composable
internal fun TalentDetailDialog(talentId: String, onDismiss: () -> Unit) {
    var currentTalentId by remember(talentId) { mutableStateOf(talentId) }
    val talent = RulesRepository.talentById(currentTalentId) ?: return
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.85f).dp
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(talent.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    buildString {
                        append(formatActivation(talent.activationType))
                        if (talent.isKey) append(" • Key Talent")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(talent.summary, style = MaterialTheme.typography.bodyMedium)

                val pathName = RulesRepository.pathById(talent.pathId)?.name ?: talent.pathId
                Text("Path: $pathName", style = MaterialTheme.typography.bodyMedium)
                talent.specialty?.let { Text("Specialty: $it", style = MaterialTheme.typography.bodyMedium) }
                talent.focusCost?.let { Text("Focus Cost: $it", style = MaterialTheme.typography.bodyMedium) }

                val otherPrerequisites = buildList {
                    if (talent.prerequisiteSkills.isNotEmpty()) {
                        add(
                            "Skills: " + talent.prerequisiteSkills.entries.joinToString {
                                "${formatSkillId(it.key)} ${it.value}"
                            },
                        )
                    }
                    talent.prerequisiteLevel?.let { add("Level: $it") }
                    talent.prerequisiteIdealSpoken?.let { add("Requires Ideal $it spoken") }
                    talent.prerequisiteOther?.let { add(it) }
                }
                if (otherPrerequisites.isEmpty() && talent.prerequisiteTalents.isEmpty()) {
                    Text("Prerequisites: None", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("Prerequisites:", style = MaterialTheme.typography.bodyMedium)
                    otherPrerequisites.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    if (talent.prerequisiteTalents.isNotEmpty()) {
                        val mode = if (talent.prerequisiteTalentsMode.equals("any", ignoreCase = true)) "any of" else "all of"
                        Text("• Talents ($mode):", style = MaterialTheme.typography.bodyMedium)
                        talent.prerequisiteTalents.forEach { prereqId ->
                            val prereqName = RulesRepository.talentById(prereqId)?.name ?: prereqId
                            SheetLink("◦ $prereqName", onClick = { currentTalentId = prereqId })
                        }
                    }
                }

                talent.page?.let {
                    Text(
                        "Page $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatActivation(activation: Activation): String = when (activation) {
    Activation.ACTION1 -> "Action (1)"
    Activation.ACTION2 -> "Action (2)"
    Activation.ACTION3 -> "Action (3)"
    Activation.FREE -> "Free Action"
    Activation.REACTION -> "Reaction"
    Activation.SPECIAL -> "Special"
    Activation.PASSIVE -> "Passive"
}

private fun formatSkillId(raw: String): String =
    raw.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
