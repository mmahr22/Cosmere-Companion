package com.cosmere.companion.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cosmere.companion.core.data.RulesRepository
import com.cosmere.companion.core.model.Attribute
import com.cosmere.companion.core.model.CharacterMath
import com.cosmere.companion.core.model.Defense
import com.cosmere.companion.core.model.GamePath
import com.cosmere.companion.core.model.PlayerCharacter
import com.cosmere.companion.core.model.Skill

@Composable
fun CharactersScreen(viewModel: CharacterViewModel = viewModel()) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val existing by viewModel.character.collectAsStateWithLifecycle()

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (existing == null) {
        CharacterCreationForm(onCreate = viewModel::save)
    } else {
        CharacterSheet(
            character = existing!!,
            onUpdate = viewModel::save,
            onReset = viewModel::reset,
        )
    }
}

@Composable
private fun CharacterCreationForm(onCreate: (PlayerCharacter) -> Unit) {
    val heroicPaths = remember { RulesRepository.paths.filter { it.type == "heroic" } }
    val radiantPaths = remember { RulesRepository.paths.filter { it.type == "radiant" } }

    var name by remember { mutableStateOf("") }
    var ancestry by remember { mutableStateOf("") }
    var culture by remember { mutableStateOf("") }
    val attributes = remember {
        mutableStateMapOf<Attribute, Int>().apply { Attribute.entries.forEach { put(it, 0) } }
    }
    var heroicPathId by remember { mutableStateOf<String?>(null) }
    var specialty by remember { mutableStateOf<String?>(null) }
    var isRadiant by remember { mutableStateOf(false) }
    var radiantPathId by remember { mutableStateOf<String?>(null) }
    val skillRanks = remember { mutableStateMapOf<String, Int>() }

    val heroicPath = heroicPaths.firstOrNull { it.id == heroicPathId }
    val radiantPath = radiantPaths.firstOrNull { it.id == radiantPathId }
    val creationSkillCap = CharacterMath.maxSkillRank(1)

    val attributePointsRemaining = CharacterMath.CREATION_ATTRIBUTE_POINTS - attributes.values.sum()
    val skillPointsRemaining = CharacterMath.CREATION_FREE_SKILL_RANKS - Skill.entries.sumOf { skill ->
        val autoMinimum = if (skill.name == heroicPath?.startingSkillId) 1 else 0
        ((skillRanks[skill.name] ?: 0) - autoMinimum).coerceAtLeast(0)
    }

    val canCreate = name.isNotBlank() &&
        heroicPathId != null &&
        attributePointsRemaining == 0 &&
        skillPointsRemaining == 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Create Your Character", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Character name") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = ancestry,
            onValueChange = { ancestry = it },
            label = { Text("Ancestry (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = culture,
            onValueChange = { culture = it },
            label = { Text("Culture (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider()

        Text(
            "Attributes ($attributePointsRemaining points remaining)",
            style = MaterialTheme.typography.titleMedium,
        )
        Attribute.entries.forEach { attribute ->
            val value = attributes[attribute] ?: 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${attribute.displayName} (${attribute.abbreviation})")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (value > 0) attributes[attribute] = value - 1 },
                        enabled = value > 0,
                    ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease ${attribute.displayName}") }
                    Text("$value", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    IconButton(
                        onClick = {
                            if (value < CharacterMath.CREATION_ATTRIBUTE_MAX && attributePointsRemaining > 0) {
                                attributes[attribute] = value + 1
                            }
                        },
                        enabled = value < CharacterMath.CREATION_ATTRIBUTE_MAX && attributePointsRemaining > 0,
                    ) { Icon(Icons.Filled.Add, contentDescription = "Increase ${attribute.displayName}") }
                }
            }
        }

        HorizontalDivider()

        Text("Heroic Path", style = MaterialTheme.typography.titleMedium)
        heroicPaths.forEach { path ->
            SelectablePathRow(
                path = path,
                selected = heroicPathId == path.id,
                onClick = {
                    heroicPath?.startingSkillId?.let { old ->
                        if ((skillRanks[old] ?: 0) <= 1) skillRanks.remove(old) else skillRanks[old] = (skillRanks[old] ?: 1) - 1
                    }
                    heroicPathId = path.id
                    specialty = null
                    path.startingSkillId?.let { skillId ->
                        skillRanks[skillId] = maxOf(skillRanks[skillId] ?: 0, 1)
                    }
                },
            )
        }
        if (heroicPath != null && heroicPath.specialties.isNotEmpty()) {
            Text("Specialty", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                heroicPath.specialties.forEach { spec ->
                    FilterChip(
                        selected = specialty == spec,
                        onClick = { specialty = spec },
                        label = { Text(spec) },
                    )
                }
            }
        }

        HorizontalDivider()

        Text(
            "Skills ($skillPointsRemaining free ranks remaining)",
            style = MaterialTheme.typography.titleMedium,
        )
        Attribute.entries.forEach { attribute ->
            Text(attribute.displayName, style = MaterialTheme.typography.labelLarge)
            Skill.forAttribute(attribute).forEach { skill ->
                val autoMinimum = if (skill.name == heroicPath?.startingSkillId) 1 else 0
                val rank = skillRanks[skill.name] ?: 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(skill.displayName + if (autoMinimum > 0) " (path)" else "")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (rank > autoMinimum) skillRanks[skill.name] = rank - 1 },
                            enabled = rank > autoMinimum,
                        ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease ${skill.displayName}") }
                        Text("$rank", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                        IconButton(
                            onClick = {
                                if (rank < creationSkillCap && skillPointsRemaining > 0) {
                                    skillRanks[skill.name] = rank + 1
                                }
                            },
                            enabled = rank < creationSkillCap && skillPointsRemaining > 0,
                        ) { Icon(Icons.Filled.Add, contentDescription = "Increase ${skill.displayName}") }
                    }
                }
            }
        }

        HorizontalDivider()

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(
                checked = isRadiant,
                onCheckedChange = { checked ->
                    isRadiant = checked
                    if (!checked) {
                        radiantPath?.surgeIds?.forEach { surgeId ->
                            if ((skillRanks[surgeId] ?: 0) <= 1) skillRanks.remove(surgeId) else skillRanks[surgeId] = (skillRanks[surgeId] ?: 1) - 1
                        }
                        radiantPathId = null
                    }
                },
            )
            Text("Already bonded to a spren (Radiant)")
        }
        if (isRadiant) {
            Text("Radiant Order", style = MaterialTheme.typography.titleMedium)
            radiantPaths.forEach { path ->
                SelectablePathRow(
                    path = path,
                    selected = radiantPathId == path.id,
                    onClick = {
                        radiantPath?.surgeIds?.forEach { old ->
                            if ((skillRanks[old] ?: 0) <= 1) skillRanks.remove(old) else skillRanks[old] = (skillRanks[old] ?: 1) - 1
                        }
                        radiantPathId = path.id
                        path.surgeIds.forEach { surgeId ->
                            skillRanks[surgeId] = maxOf(skillRanks[surgeId] ?: 0, 1)
                        }
                    },
                )
            }
        }

        Button(
            onClick = {
                onCreate(
                    PlayerCharacter(
                        name = name.trim(),
                        ancestry = ancestry.trim(),
                        culture = culture.trim(),
                        attributes = attributes.toMap(),
                        heroicPathId = heroicPathId!!,
                        specialty = specialty,
                        radiantPathId = radiantPathId,
                        skillRanks = skillRanks.toMap(),
                    ),
                )
            },
            enabled = canCreate,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Create Character")
        }
    }
}

@Composable
private fun SelectablePathRow(path: GamePath, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                path.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                path.summary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CharacterSheet(
    character: PlayerCharacter,
    onUpdate: (PlayerCharacter) -> Unit,
    onReset: () -> Unit,
) {
    val heroicPath = remember(character.heroicPathId) { RulesRepository.pathById(character.heroicPathId) }
    val radiantPath = remember(character.radiantPathId) {
        character.radiantPathId?.let { RulesRepository.pathById(it) }
    }

    fun updateSkillRank(skillId: String, newRank: Int) {
        val clamped = newRank.coerceIn(0, CharacterMath.maxSkillRank(character.level))
        onUpdate(character.copy(skillRanks = character.skillRanks + (skillId to clamped)))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(character.name, style = MaterialTheme.typography.headlineSmall)
        if (character.ancestry.isNotBlank() || character.culture.isNotBlank()) {
            Text(
                listOf(character.ancestry, character.culture).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        val pathLine = buildString {
            append(heroicPath?.name ?: character.heroicPathId)
            character.specialty?.let { append(" — $it") }
        }
        Text(pathLine, style = MaterialTheme.typography.bodyMedium)
        radiantPath?.let { rp ->
            Text("${rp.name} · Bonded to ${rp.sprenType ?: "a spren"}", style = MaterialTheme.typography.bodyMedium)
        }

        HorizontalDivider()

        ResourceTracker(
            label = "Health",
            current = character.currentHealth,
            max = character.maxHealth,
            onCurrentChange = { onUpdate(character.copy(currentHealth = it.coerceIn(0, character.maxHealth))) },
        )
        ResourceTracker(
            label = "Focus",
            current = character.currentFocus,
            max = character.maxFocus,
            onCurrentChange = { onUpdate(character.copy(currentFocus = it.coerceIn(0, character.maxFocus))) },
        )
        ResourceTracker(
            label = "Investiture",
            current = character.currentInvestiture,
            max = character.maxInvestiture,
            onCurrentChange = { onUpdate(character.copy(currentInvestiture = it.coerceIn(0, character.maxInvestiture))) },
            onMaxChange = { newMax ->
                val clampedMax = newMax.coerceAtLeast(0)
                onUpdate(
                    character.copy(
                        maxInvestiture = clampedMax,
                        currentInvestiture = character.currentInvestiture.coerceAtMost(clampedMax),
                    ),
                )
            },
        )

        HorizontalDivider()

        Text("Defenses", style = MaterialTheme.typography.titleMedium)
        Defense.entries.forEach { defense ->
            val pair = Attribute.entries.filter { it.defense == defense }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${defense.displayName} (${pair.joinToString(" + ") { it.abbreviation }})")
                Text("${character.defense(defense)}", fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider()

        Text("Skills", style = MaterialTheme.typography.titleMedium)
        val skillCap = CharacterMath.maxSkillRank(character.level)
        Attribute.entries.forEach { attribute ->
            Text(attribute.displayName, style = MaterialTheme.typography.labelLarge)
            Skill.forAttribute(attribute).forEach { skill ->
                val rank = character.skillRank(skill.name)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(skill.displayName)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { updateSkillRank(skill.name, rank - 1) },
                            enabled = rank > 0,
                        ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease ${skill.displayName}") }
                        Text("$rank", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                        IconButton(
                            onClick = { updateSkillRank(skill.name, rank + 1) },
                            enabled = rank < skillCap,
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

        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Text("New Character")
        }
    }
}

@Composable
private fun ResourceTracker(
    label: String,
    current: Int,
    max: Int,
    onCurrentChange: (Int) -> Unit,
    onMaxChange: ((Int) -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text("$current / $max", style = MaterialTheme.typography.titleMedium)
            }
            LinearProgressIndicator(
                progress = { if (max > 0) current.toFloat() / max else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { if (current > 0) onCurrentChange(current - 1) },
                    enabled = current > 0,
                ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease $label") }
                IconButton(
                    onClick = { if (current < max) onCurrentChange(current + 1) },
                    enabled = current < max,
                ) { Icon(Icons.Filled.Add, contentDescription = "Increase $label") }
                if (onMaxChange != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Max:", style = MaterialTheme.typography.bodySmall)
                    IconButton(
                        onClick = { if (max > 0) onMaxChange(max - 1) },
                        enabled = max > 0,
                    ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease max $label") }
                    IconButton(onClick = { onMaxChange(max + 1) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase max $label")
                    }
                }
            }
        }
    }
}
