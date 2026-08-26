package com.cosmere.companion.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cosmere.companion.core.data.RulesRepository
import com.cosmere.companion.core.dice.DamageRollResult
import com.cosmere.companion.core.model.Item
import com.cosmere.companion.core.model.ItemType
import com.cosmere.companion.core.model.PlayerCharacter
import com.cosmere.companion.core.model.Skill

@Composable
internal fun InventorySection(character: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
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
                val newWeapons = when {
                    item.id in character.equippedWeaponIds -> character.equippedWeaponIds - item.id
                    character.canWieldAdditionalWeapon(item.id) -> character.equippedWeaponIds + item.id
                    else -> character.equippedWeaponIds
                }
                onUpdate(character.copy(equippedWeaponIds = newWeapons))
            }
            else -> Unit
        }
    }

    var showAddItem by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Inventory", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { showAddItem = true }) {
            Icon(Icons.Filled.Add, contentDescription = "Add item")
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Equipped", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "Armor: ${equippedArmor?.name ?: "None"}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (equippedWeapons.isEmpty()) {
                Text("Weapons: None", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Weapons:", style = MaterialTheme.typography.bodySmall)
                equippedWeapons.forEach { weapon ->
                    EquippedWeaponRow(item = weapon, character = character)
                }
            }
        }
    }

    Text(
        "Carrying ${formatWeight(character.carriedWeightLb)} / ${character.carryingCapacityLb} lb." +
            if (character.isOverCarryingCapacity) " — over capacity" else "",
        style = MaterialTheme.typography.bodySmall,
        color = if (character.isOverCarryingCapacity) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )

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
                canEquip = equipped || item.type != ItemType.WEAPON || character.canWieldAdditionalWeapon(item.id),
                onQuantityChange = { setQuantity(item, it) },
                onToggleEquip = if (item.type == ItemType.WEAPON || item.type == ItemType.ARMOR) {
                    { toggleEquip(item) }
                } else {
                    null
                },
            )
        }
    }

    if (showAddItem) {
        AddItemDialog(
            character = character,
            onAdd = { item -> setQuantity(item, character.inventoryQuantity(item.id) + 1) },
            onDismiss = { showAddItem = false },
        )
    }
}

@Composable
private fun AddItemDialog(character: PlayerCharacter, onAdd: (Item) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val matches = remember(query) {
        if (query.isBlank()) {
            emptyList()
        } else {
            RulesRepository.items.filter { it.name.contains(query, ignoreCase = true) }.sortedBy { it.name }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Add Item", style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text("Search weapons, armor, equipment, fabrials…") },
                    singleLine = true,
                )
                HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    if (query.isBlank()) {
                        Text(
                            "Start typing to search the item list.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (matches.isEmpty()) {
                        Text("No matching items.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        matches.forEach { item ->
                            val carrying = character.inventoryQuantity(item.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAdd(item) }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        itemSubtitle(item) + if (carrying > 0) " • Carrying $carrying" else "",
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
        }
    }
}

@Composable
private fun InventoryItemRow(
    item: Item,
    quantity: Int,
    equipped: Boolean,
    canEquip: Boolean,
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
                enabled = canEquip,
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

/**
 * An equipped weapon's name plus inline attack/damage roll triggers — attack rolls a skill
 * test against the weapon's governing skill (see [Item.skill]); damage rolls the weapon's own
 * dice (see [Item.damage]) with no attribute bonus, matching the book's flat damage dice. Either
 * roll is omitted if the item's text doesn't resolve to a known skill or a `NdN` dice expression
 * (e.g. "Same as similar weapon", "Unique").
 */
@Composable
private fun EquippedWeaponRow(item: Item, character: PlayerCharacter) {
    val skill = remember(item.skill) { item.skill?.let { name -> Skill.entries.find { it.displayName == name } } }
    val attackModifier = skill?.let { character.effectiveAttribute(it.attribute) + character.skillRank(it.name) }
    val damageSpec = remember(item.damage) { item.damage?.let(::parseDamageDie) }

    var attackRollState by remember(item.id) { mutableStateOf<SkillRollUiState>(SkillRollUiState.Collapsed) }
    var damageResult by remember(item.id) { mutableStateOf<DamageRollResult?>(null) }

    Column(modifier = Modifier.padding(start = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            if (attackModifier != null) {
                SkillRollTrigger(
                    label = "${item.name} attack",
                    modifierValue = attackModifier,
                    onStateChange = { attackRollState = it },
                )
            }
            damageSpec?.let { (die, count) ->
                DamageRollTrigger(
                    label = item.name,
                    die = die,
                    count = count,
                    onResult = { damageResult = it },
                )
            }
        }
        weaponQuickStats(item)?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (attackModifier != null) {
            SkillRollExpansion(modifierValue = attackModifier, state = attackRollState, onStateChange = { attackRollState = it })
        }
        DamageRollExpansion(result = damageResult, onDismiss = { damageResult = null })
    }
}

/** A one-line combat quick-reference for an equipped weapon, or null if it carries no such stats. */
private fun weaponQuickStats(item: Item): String? {
    val parts = buildList {
        item.damage?.let { add("Damage $it") }
        item.range?.let { add("Range $it") }
        if (item.traits.isNotEmpty()) add("Traits: ${item.traits.joinToString()}")
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}

private fun itemSubtitle(item: Item): String = when (item.type) {
    ItemType.WEAPON -> "Weapon" + (item.category?.let { " • $it" } ?: "")
    ItemType.ARMOR -> "Armor • Deflect ${item.deflectValue ?: 0}"
    ItemType.FABRIAL -> "Fabrial"
    ItemType.EQUIPMENT -> "Equipment"
}

/** Trims a trailing ".0" so whole-number weights don't show a pointless decimal. */
private fun formatWeight(lb: Double): String =
    if (lb == lb.toLong().toDouble()) lb.toLong().toString() else lb.toString()
