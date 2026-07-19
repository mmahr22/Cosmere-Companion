package com.cosmere.companion.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.cosmere.companion.core.model.Ancestry
import com.cosmere.companion.core.model.Attribute
import com.cosmere.companion.core.model.CharacterMath
import com.cosmere.companion.core.model.Defense
import com.cosmere.companion.core.model.GamePath
import com.cosmere.companion.core.model.Item
import com.cosmere.companion.core.model.ItemType
import com.cosmere.companion.core.model.PlayerCharacter
import com.cosmere.companion.core.model.Skill
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val MAX_CULTURES = 2

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

/**
 * In-progress character creation state. Held as a single immutable snapshot
 * (rather than several [mutableStateOf]/[androidx.compose.runtime.mutableStateMapOf]
 * vars) so the whole form can ride one [rememberSaveable] call: switching
 * bottom-nav tabs mid-creation and coming back used to reset every field,
 * since plain `remember` state doesn't survive a composable leaving
 * composition the way `rememberSaveable` does.
 */
@Serializable
private data class CharacterDraft(
    val name: String = "",
    val ancestryId: String? = null,
    val cultureIds: List<String> = emptyList(),
    val attributes: Map<String, Int> = Attribute.entries.associate { it.name to 0 },
    val heroicPathId: String? = null,
    val specialty: String? = null,
    val isRadiant: Boolean = false,
    val radiantPathId: String? = null,
    val skillRanks: Map<String, Int> = emptyMap(),
) {
    fun attributeValue(attribute: Attribute): Int = attributes[attribute.name] ?: 0

    fun skillRank(skillId: String): Int = skillRanks[skillId] ?: 0

    fun withSkillRank(skillId: String, rank: Int): CharacterDraft =
        if (rank <= 0) copy(skillRanks = skillRanks - skillId) else copy(skillRanks = skillRanks + (skillId to rank))
}

private val CharacterDraftSaver = Saver<CharacterDraft, String>(
    save = { Json.encodeToString(it) },
    restore = { Json.decodeFromString(it) },
)

@Composable
private fun CharacterCreationForm(onCreate: (PlayerCharacter) -> Unit) {
    val heroicPaths = remember { RulesRepository.paths.filter { it.type == "heroic" } }
    val radiantPaths = remember { RulesRepository.paths.filter { it.type == "radiant" } }

    var draft by rememberSaveable(stateSaver = CharacterDraftSaver) { mutableStateOf(CharacterDraft()) }

    val heroicPath = heroicPaths.firstOrNull { it.id == draft.heroicPathId }
    val radiantPath = radiantPaths.firstOrNull { it.id == draft.radiantPathId }
    val creationSkillCap = CharacterMath.maxSkillRank(1)

    val attributePointsRemaining = CharacterMath.CREATION_ATTRIBUTE_POINTS - draft.attributes.values.sum()
    val skillPointsRemaining = CharacterMath.CREATION_FREE_SKILL_RANKS - Skill.entries.sumOf { skill ->
        val autoMinimum = if (skill.name == heroicPath?.startingSkillId) 1 else 0
        (draft.skillRank(skill.name) - autoMinimum).coerceAtLeast(0)
    }

    val canCreate = draft.name.isNotBlank() &&
        draft.ancestryId != null &&
        draft.heroicPathId != null &&
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
            value = draft.name,
            onValueChange = { draft = draft.copy(name = it) },
            label = { Text("Character name") },
            modifier = Modifier.fillMaxWidth(),
        )
        HorizontalDivider()

        Text("Ancestry", style = MaterialTheme.typography.titleMedium)
        RulesRepository.ancestries.forEach { ancestry ->
            SelectableAncestryRow(
                ancestry = ancestry,
                selected = draft.ancestryId == ancestry.id,
                onClick = {
                    draft = draft.copy(
                        ancestryId = ancestry.id,
                        cultureIds = if (ancestry.id != "singer") draft.cultureIds - "listener" else draft.cultureIds,
                    )
                },
            )
        }

        Text(
            "Culture (choose up to $MAX_CULTURES)",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RulesRepository.cultures
                .filter { !it.singerOnly || draft.ancestryId == "singer" }
                .forEach { cultureOption ->
                    val selected = cultureOption.id in draft.cultureIds
                    FilterChip(
                        selected = selected,
                        onClick = {
                            draft = when {
                                selected -> draft.copy(cultureIds = draft.cultureIds - cultureOption.id)
                                draft.cultureIds.size < MAX_CULTURES ->
                                    draft.copy(cultureIds = draft.cultureIds + cultureOption.id)
                                else -> draft
                            }
                        },
                        label = { Text(cultureOption.name) },
                    )
                }
        }

        HorizontalDivider()

        Text(
            "Attributes ($attributePointsRemaining points remaining)",
            style = MaterialTheme.typography.titleMedium,
        )
        Attribute.entries.forEach { attribute ->
            val value = draft.attributeValue(attribute)
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
                                draft = draft.copy(attributes = draft.attributes + (attribute.name to value - 1))
                            }
                        },
                        enabled = value > 0,
                    ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease ${attribute.displayName}") }
                    Text("$value", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    IconButton(
                        onClick = {
                            if (value < CharacterMath.CREATION_ATTRIBUTE_MAX && attributePointsRemaining > 0) {
                                draft = draft.copy(attributes = draft.attributes + (attribute.name to value + 1))
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
                selected = draft.heroicPathId == path.id,
                onClick = {
                    var skillRanks = draft.skillRanks
                    heroicPath?.startingSkillId?.let { old ->
                        val current = skillRanks[old] ?: 0
                        skillRanks = if (current <= 1) skillRanks - old else skillRanks + (old to current - 1)
                    }
                    path.startingSkillId?.let { skillId ->
                        skillRanks = skillRanks + (skillId to maxOf(skillRanks[skillId] ?: 0, 1))
                    }
                    draft = draft.copy(heroicPathId = path.id, specialty = null, skillRanks = skillRanks)
                },
            )
        }
        if (heroicPath != null && heroicPath.specialties.isNotEmpty()) {
            Text("Specialty", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                heroicPath.specialties.forEach { spec ->
                    FilterChip(
                        selected = draft.specialty == spec,
                        onClick = { draft = draft.copy(specialty = spec) },
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
                val rank = draft.skillRank(skill.name)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(skill.displayName + if (autoMinimum > 0) " (path)" else "")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (rank > autoMinimum) draft = draft.withSkillRank(skill.name, rank - 1)
                            },
                            enabled = rank > autoMinimum,
                        ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease ${skill.displayName}") }
                        Text("$rank", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                        IconButton(
                            onClick = {
                                if (rank < creationSkillCap && skillPointsRemaining > 0) {
                                    draft = draft.withSkillRank(skill.name, rank + 1)
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
                checked = draft.isRadiant,
                onCheckedChange = { checked ->
                    draft = if (checked) {
                        draft.copy(isRadiant = true)
                    } else {
                        var skillRanks = draft.skillRanks
                        radiantPath?.surgeIds?.forEach { surgeId ->
                            val current = skillRanks[surgeId] ?: 0
                            skillRanks = if (current <= 1) skillRanks - surgeId else skillRanks + (surgeId to current - 1)
                        }
                        draft.copy(isRadiant = false, radiantPathId = null, skillRanks = skillRanks)
                    }
                },
            )
            Text("Already bonded to a spren (Radiant)")
        }
        if (draft.isRadiant) {
            Text("Radiant Order", style = MaterialTheme.typography.titleMedium)
            radiantPaths.forEach { path ->
                SelectablePathRow(
                    path = path,
                    selected = draft.radiantPathId == path.id,
                    onClick = {
                        var skillRanks = draft.skillRanks
                        radiantPath?.surgeIds?.forEach { old ->
                            val current = skillRanks[old] ?: 0
                            skillRanks = if (current <= 1) skillRanks - old else skillRanks + (old to current - 1)
                        }
                        path.surgeIds.forEach { surgeId ->
                            skillRanks = skillRanks + (surgeId to maxOf(skillRanks[surgeId] ?: 0, 1))
                        }
                        draft = draft.copy(radiantPathId = path.id, skillRanks = skillRanks)
                    },
                )
            }
        }

        Button(
            onClick = {
                onCreate(
                    PlayerCharacter(
                        name = draft.name.trim(),
                        ancestryId = draft.ancestryId,
                        cultureIds = draft.cultureIds,
                        attributes = Attribute.entries.associateWith { draft.attributeValue(it) },
                        heroicPathId = draft.heroicPathId!!,
                        specialty = draft.specialty,
                        radiantPathId = draft.radiantPathId,
                        skillRanks = draft.skillRanks,
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
private fun SelectableAncestryRow(ancestry: Ancestry, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                ancestry.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                ancestry.summary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
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
    val ancestry = remember(character.ancestryId) {
        character.ancestryId?.let { RulesRepository.ancestryById(it) }
    }
    val cultures = remember(character.cultureIds) {
        character.cultureIds.mapNotNull { RulesRepository.cultureById(it) }
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
        if (ancestry != null || cultures.isNotEmpty()) {
            Text(
                (listOfNotNull(ancestry?.name) + cultures.map { it.name }).joinToString(" · "),
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

        if (character.ancestryId == "singer") {
            HorizontalDivider()
            SingerFormsSection(character = character, onUpdate = onUpdate)
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

        HorizontalDivider()
        InventorySection(character = character, onUpdate = onUpdate)

        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Text("New Character")
        }
    }
}

@Composable
private fun InventorySection(character: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    val itemsById = remember { RulesRepository.items.associateBy { it.id } }
    val equippedArmor = character.equippedArmorId?.let { itemsById[it] }
    val equippedWeapons = character.equippedWeaponIds.mapNotNull { itemsById[it] }
    val ownedEntries = remember(character.inventory) {
        character.inventory.entries
            .filter { it.value > 0 }
            .mapNotNull { (id, qty) -> itemsById[id]?.let { it to qty } }
            .sortedBy { it.first.name }
    }

    fun setQuantity(item: Item, newQuantity: Int) {
        val clamped = newQuantity.coerceAtLeast(0)
        val newInventory = if (clamped == 0) character.inventory - item.id else character.inventory + (item.id to clamped)
        var updated = character.copy(inventory = newInventory)
        if (clamped == 0) {
            if (updated.equippedArmorId == item.id) updated = updated.copy(equippedArmorId = null)
            if (item.id in updated.equippedWeaponIds) {
                updated = updated.copy(equippedWeaponIds = updated.equippedWeaponIds - item.id)
            }
        }
        onUpdate(updated)
    }

    fun toggleEquip(item: Item) {
        when (item.type) {
            ItemType.ARMOR -> onUpdate(
                character.copy(equippedArmorId = if (character.equippedArmorId == item.id) null else item.id),
            )
            ItemType.WEAPON -> {
                val newWeapons = if (item.id in character.equippedWeaponIds) {
                    character.equippedWeaponIds - item.id
                } else {
                    character.equippedWeaponIds + item.id
                }
                onUpdate(character.copy(equippedWeaponIds = newWeapons))
            }
            else -> Unit
        }
    }

    Text("Inventory", style = MaterialTheme.typography.titleMedium)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Equipped", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "Armor: ${equippedArmor?.name ?: "None"} (Deflect ${character.deflectValue})",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Weapons: " + if (equippedWeapons.isEmpty()) "None" else equippedWeapons.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (ownedEntries.isEmpty()) {
        Text("No items carried yet.", style = MaterialTheme.typography.bodySmall)
    } else {
        Text("Carried Items", style = MaterialTheme.typography.labelLarge)
        ownedEntries.forEach { (item, quantity) ->
            val equipped = item.id == character.equippedArmorId || item.id in character.equippedWeaponIds
            InventoryItemRow(
                item = item,
                quantity = quantity,
                equipped = equipped,
                onQuantityChange = { setQuantity(item, it) },
                onToggleEquip = if (item.type == ItemType.WEAPON || item.type == ItemType.ARMOR) {
                    { toggleEquip(item) }
                } else {
                    null
                },
            )
        }
    }

    HorizontalDivider()

    Text("Add Item", style = MaterialTheme.typography.labelLarge)
    var addQuery by remember { mutableStateOf("") }
    OutlinedTextField(
        value = addQuery,
        onValueChange = { addQuery = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search weapons, armor, equipment, fabrials…") },
        singleLine = true,
    )
    if (addQuery.isNotBlank()) {
        val matches = remember(addQuery) {
            RulesRepository.items.filter { it.name.contains(addQuery, ignoreCase = true) }.take(8)
        }
        if (matches.isEmpty()) {
            Text("No matching items.", style = MaterialTheme.typography.bodySmall)
        } else {
            matches.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            setQuantity(item, character.inventoryQuantity(item.id) + 1)
                            addQuery = ""
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            itemSubtitle(item),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Filled.Add, contentDescription = "Add ${item.name}")
                }
            }
        }
    }
}

@Composable
private fun InventoryItemRow(
    item: Item,
    quantity: Int,
    equipped: Boolean,
    onQuantityChange: (Int) -> Unit,
    onToggleEquip: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, fontWeight = if (equipped) FontWeight.Bold else FontWeight.Normal)
            Text(
                itemSubtitle(item),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onToggleEquip != null) {
            FilterChip(
                selected = equipped,
                onClick = onToggleEquip,
                label = { Text(if (equipped) "Equipped" else "Equip") },
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        IconButton(onClick = { onQuantityChange(quantity - 1) }, enabled = quantity > 0) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease ${item.name} quantity")
        }
        Text("$quantity", modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onQuantityChange(quantity + 1) }) {
            Icon(Icons.Filled.Add, contentDescription = "Increase ${item.name} quantity")
        }
    }
}

private fun itemSubtitle(item: Item): String = when (item.type) {
    ItemType.WEAPON -> "Weapon" + (item.category?.let { " • $it" } ?: "")
    ItemType.ARMOR -> "Armor • Deflect ${item.deflectValue ?: 0}"
    ItemType.FABRIAL -> "Fabrial"
    ItemType.EQUIPMENT -> "Equipment"
}

@Composable
private fun SingerFormsSection(character: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
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
