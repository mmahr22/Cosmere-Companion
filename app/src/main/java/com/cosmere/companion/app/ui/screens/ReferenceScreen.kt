package com.cosmere.companion.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cosmere.companion.core.data.RulesRepository
import com.cosmere.companion.core.model.Activation
import com.cosmere.companion.core.model.Ancestry
import com.cosmere.companion.core.model.Condition
import com.cosmere.companion.core.model.Culture
import com.cosmere.companion.core.model.Attribute
import com.cosmere.companion.core.model.GamePath
import com.cosmere.companion.core.model.Item
import com.cosmere.companion.core.model.ItemType
import com.cosmere.companion.core.model.SingerForm
import com.cosmere.companion.core.model.SurgeEntry
import com.cosmere.companion.core.model.SurgeTalent
import com.cosmere.companion.core.model.Talent

/** Top-level category used for filtering the reference browser. */
private enum class ReferenceCategory(val label: String) {
    ALL("All"),
    ANCESTRIES("Ancestries"),
    CULTURES("Cultures"),
    PATHS("Paths"),
    TALENTS("Talents"),
    SINGER_FORMS("Forms"),
    SURGES("Surges"),
    CONDITIONS("Conditions"),
    ITEMS("Items"),
}

/**
 * Builders for [ReferenceEntry.key] values, shared with call sites outside this file
 * (e.g. `CharactersScreen`) that need to link into a specific Reference entry.
 */
fun pathReferenceKey(id: String): String = "path:$id"
fun talentReferenceKey(id: String): String = "talent:$id"
fun surgeReferenceKey(id: String): String = "surge:$id"
fun ancestryReferenceKey(id: String): String = "ancestry:$id"
fun cultureReferenceKey(id: String): String = "culture:$id"

/**
 * A unifying wrapper so paths, talents, surges, and conditions can share one
 * searchable/filterable list even though their underlying shapes differ.
 */
private sealed interface ReferenceEntry {
    val key: String
    val name: String
    val subtitle: String
    val summary: String
    val category: ReferenceCategory
    val page: Int?

    data class PathItem(val path: GamePath) : ReferenceEntry {
        override val key: String = pathReferenceKey(path.id)
        override val name: String = path.name
        override val subtitle: String = "${path.type.replaceFirstChar { it.uppercase() }} Path"
        override val summary: String = path.summary
        override val category: ReferenceCategory = ReferenceCategory.PATHS
        override val page: Int? = path.page
    }

    data class TalentItem(val talent: Talent) : ReferenceEntry {
        override val key: String = talentReferenceKey(talent.id)
        override val name: String = talent.name
        override val subtitle: String = buildString {
            append(formatActivation(talent.activationType))
            if (talent.isKey) append(" • Key Talent")
        }
        override val summary: String = talent.summary
        override val category: ReferenceCategory = ReferenceCategory.TALENTS
        override val page: Int? = talent.page
    }

    data class SurgeItem(val surge: SurgeEntry) : ReferenceEntry {
        override val key: String = surgeReferenceKey(surge.id)
        override val name: String = surge.name
        override val subtitle: String = "Surge • ${formatIdentifier(surge.attributeId)}"
        override val summary: String = surge.summary
        override val category: ReferenceCategory = ReferenceCategory.SURGES
        override val page: Int? = surge.page
    }

    data class ConditionItem(val condition: Condition) : ReferenceEntry {
        override val key: String = "condition:${condition.id}"
        override val name: String = condition.name
        override val subtitle: String = "Condition"
        override val summary: String = condition.summary
        override val category: ReferenceCategory = ReferenceCategory.CONDITIONS
        override val page: Int? = condition.page
    }

    data class AncestryItem(val ancestry: Ancestry) : ReferenceEntry {
        override val key: String = ancestryReferenceKey(ancestry.id)
        override val name: String = ancestry.name
        override val subtitle: String = "Ancestry • Size ${ancestry.size}"
        override val summary: String = ancestry.summary
        override val category: ReferenceCategory = ReferenceCategory.ANCESTRIES
        override val page: Int? = ancestry.page
    }

    data class CultureItem(val culture: Culture) : ReferenceEntry {
        override val key: String = cultureReferenceKey(culture.id)
        override val name: String = culture.name
        override val subtitle: String = "Culture" + if (culture.singerOnly) " • Singer only" else ""
        override val summary: String = culture.summary
        override val category: ReferenceCategory = ReferenceCategory.CULTURES
        override val page: Int? = culture.page
    }

    data class SingerFormItem(val form: SingerForm) : ReferenceEntry {
        override val key: String = "singer_form:${form.id}"
        override val name: String = form.name
        override val subtitle: String = "Form • Bonded to ${form.sprenBond}" + if (form.voidform) " (Voidspren)" else ""
        override val summary: String = form.summary
        override val category: ReferenceCategory = ReferenceCategory.SINGER_FORMS
        override val page: Int? = form.page
    }

    data class ItemEntry(val item: Item) : ReferenceEntry {
        override val key: String = "item:${item.id}"
        override val name: String = item.name
        override val subtitle: String = when (item.type) {
            ItemType.WEAPON -> "Weapon • ${item.category}"
            ItemType.ARMOR -> "Armor"
            ItemType.FABRIAL -> "Fabrial"
            ItemType.EQUIPMENT -> "Equipment"
        }
        override val summary: String = item.summary
        override val category: ReferenceCategory = ReferenceCategory.ITEMS
        override val page: Int? = item.page
    }
}

private fun buildReferenceEntries(): List<ReferenceEntry> {
    // The "singer" entry in paths.json only exists so the Singer talent tree has a
    // home for keyTalentId/talentsForPath lookups; it's surfaced to players as an
    // Ancestry entry instead, not a second Paths entry.
    val paths = RulesRepository.paths.filter { it.type != "ancestry" }.map { ReferenceEntry.PathItem(it) }
    val talents = RulesRepository.talents.map { ReferenceEntry.TalentItem(it) }
    val surges = RulesRepository.surges.map { ReferenceEntry.SurgeItem(it) }
    val conditions = RulesRepository.conditions.map { ReferenceEntry.ConditionItem(it) }
    val ancestries = RulesRepository.ancestries.map { ReferenceEntry.AncestryItem(it) }
    val cultures = RulesRepository.cultures.map { ReferenceEntry.CultureItem(it) }
    val singerForms = RulesRepository.singerForms.map { ReferenceEntry.SingerFormItem(it) }
    val items = RulesRepository.items.map { ReferenceEntry.ItemEntry(it) }
    return ancestries + cultures + paths + talents + singerForms + surges + conditions + items
}

private fun formatIdentifier(raw: String): String =
    raw.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun formatActivation(activation: Activation): String = when (activation) {
    Activation.ACTION1 -> "Action (1)"
    Activation.ACTION2 -> "Action (2)"
    Activation.ACTION3 -> "Action (3)"
    Activation.FREE -> "Free Action"
    Activation.REACTION -> "Reaction"
    Activation.SPECIAL -> "Special"
    Activation.PASSIVE -> "Passive"
}

@Composable
fun ReferenceScreen(focusKey: String? = null, onFocusHandled: () -> Unit = {}) {
    val entries = remember { buildReferenceEntries() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ReferenceCategory.ALL) }
    var expandedKey by remember { mutableStateOf<String?>(null) }
    var pendingScrollKey by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    val filteredEntries = remember(entries, searchQuery, selectedCategory) {
        entries.filter { entry ->
            val matchesCategory =
                selectedCategory == ReferenceCategory.ALL || entry.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                entry.name.contains(searchQuery, ignoreCase = true) ||
                entry.summary.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    // Jumps the list to a specific entry, resetting any filters that would hide it —
    // used both for cross-tab links (from the character sheet) and in-Reference links
    // (e.g. tapping a talent's path from within its detail card).
    fun jumpTo(key: String) {
        selectedCategory = ReferenceCategory.ALL
        searchQuery = ""
        expandedKey = key
        pendingScrollKey = key
    }

    LaunchedEffect(focusKey) {
        val key = focusKey ?: return@LaunchedEffect
        jumpTo(key)
        onFocusHandled()
    }

    LaunchedEffect(filteredEntries, pendingScrollKey) {
        val key = pendingScrollKey ?: return@LaunchedEffect
        val index = filteredEntries.indexOfFirst { it.key == key }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            pendingScrollKey = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Rules Reference", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search paths, talents, surges, conditions…") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            },
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReferenceCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.label) },
                )
            }
        }

        if (filteredEntries.isEmpty()) {
            Text(
                text = "No entries match your search.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredEntries, key = { it.key }) { entry ->
                    ReferenceEntryCard(
                        entry = entry,
                        expanded = expandedKey == entry.key,
                        onToggle = {
                            expandedKey = if (expandedKey == entry.key) null else entry.key
                        },
                        onLinkClick = ::jumpTo,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceEntryCard(
    entry: ReferenceEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
    onLinkClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = entry.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                )
            }

            if (!expanded) {
                Text(
                    text = entry.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(entry.summary, style = MaterialTheme.typography.bodyMedium)
                ReferenceEntryDetails(entry, onLinkClick)
                entry.page?.let { page ->
                    Text(
                        text = "Page $page",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceEntryDetails(entry: ReferenceEntry, onLinkClick: (String) -> Unit) {
    when (entry) {
        is ReferenceEntry.PathItem -> PathDetails(entry.path, onLinkClick)
        is ReferenceEntry.TalentItem -> TalentDetails(entry.talent, onLinkClick)
        is ReferenceEntry.SurgeItem -> SurgeDetails(entry.surge)
        is ReferenceEntry.ConditionItem -> ConditionDetails(entry.condition)
        is ReferenceEntry.AncestryItem -> AncestryDetails(entry.ancestry, onLinkClick)
        is ReferenceEntry.CultureItem -> CultureDetails(entry.culture)
        is ReferenceEntry.SingerFormItem -> SingerFormDetails(entry.form, onLinkClick)
        is ReferenceEntry.ItemEntry -> ItemDetails(entry.item)
    }
}

/** A path id as it should be linked to from elsewhere — the "singer" path has no Paths entry of its own (it's surfaced as an Ancestry instead). */
private fun linkKeyForPath(pathId: String): String =
    if (pathId == "singer") ancestryReferenceKey("singer") else pathReferenceKey(pathId)

@Composable
private fun LinkText(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
        modifier = modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun AncestryDetails(ancestry: Ancestry, onLinkClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val levels = ancestry.bonusTalentLevels.joinToString()
        Text("Bonus Talents (Level $levels): ${ancestry.bonusTalentSource}")
        ancestry.keyTalentId?.let { keyTalentId ->
            val keyTalentName = RulesRepository.talents.firstOrNull { it.id == keyTalentId }?.name ?: keyTalentId
            Row {
                Text("Key Talent (Level 1): ")
                LinkText(keyTalentName, onClick = { onLinkClick(talentReferenceKey(keyTalentId)) })
            }
        }
    }
}

@Composable
private fun CultureDetails(culture: Culture) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Expertise: ${culture.expertiseSummary}")
        if (culture.names.isNotEmpty()) {
            Text("Example Names: ${culture.names.joinToString()}")
        }
    }
}

@Composable
private fun SingerFormDetails(form: SingerForm, onLinkClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (form.attributeBonuses.isNotEmpty()) {
            val bonuses = form.attributeBonuses.entries.joinToString {
                val abbreviation = Attribute.entries.find { attr -> attr.name == it.key }?.abbreviation ?: it.key
                "$abbreviation +${it.value}"
            }
            Text("Attribute Bonuses: $bonuses")
        }
        val grantedBy = form.grantedByTalentId
        if (grantedBy == null) {
            Text("Granted by: Change Form (starting form)")
        } else {
            val talentName = RulesRepository.talents.firstOrNull { it.id == grantedBy }?.name ?: grantedBy
            Row {
                Text("Granted by: ")
                LinkText(talentName, onClick = { onLinkClick(talentReferenceKey(grantedBy)) })
            }
        }
    }
}

@Composable
private fun ItemDetails(item: Item) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item.skill?.let { Text("Skill: $it") }
        item.damage?.let { Text("Damage: $it") }
        item.range?.let { Text("Range: $it") }
        item.deflectValue?.let { Text("Deflect Value: $it") }
        item.charges?.let { Text("Charges: $it") }
        if (item.traits.isNotEmpty()) {
            Text("Traits: ${item.traits.joinToString()}")
        }
        if (item.expertTraits.isNotEmpty()) {
            Text("Expert Traits: ${item.expertTraits.joinToString()}")
        }
        val weightPrice = listOfNotNull(
            item.weight?.let { "Weight: $it" },
            item.price?.let { "Price: $it" },
        )
        if (weightPrice.isNotEmpty()) {
            Text(weightPrice.joinToString(" • "))
        }
    }
}

@Composable
private fun PathDetails(path: GamePath, onLinkClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (path.specialties.isNotEmpty()) {
            Text("Specialties: ${path.specialties.joinToString()}")
        }
        val keyTalentName = RulesRepository.talents.firstOrNull { it.id == path.keyTalentId }?.name
            ?: path.keyTalentId
        Row {
            Text("Key Talent: ")
            LinkText(keyTalentName, onClick = { onLinkClick(talentReferenceKey(path.keyTalentId)) })
        }
        path.startingSkillId?.let {
            Text("Starting Skill: ${formatIdentifier(it)}")
        }
        if (path.attributeTips.isNotEmpty()) {
            Text("Attribute Tips: ${path.attributeTips.joinToString { formatIdentifier(it) }}")
        }
        if (path.skillTips.isNotEmpty()) {
            Text("Skill Tips: ${path.skillTips.joinToString { formatIdentifier(it) }}")
        }
        path.sprenType?.let { Text("Spren Type: ${formatIdentifier(it)}") }
        if (path.surgeIds.isNotEmpty()) {
            Text("Surges:")
            path.surgeIds.forEach { surgeId ->
                val surgeName = RulesRepository.surgeById(surgeId)?.name ?: formatIdentifier(surgeId)
                LinkText(
                    "• $surgeName",
                    onClick = { onLinkClick(surgeReferenceKey(surgeId)) },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TalentDetails(talent: Talent, onLinkClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val pathName = RulesRepository.pathById(talent.pathId)?.name ?: talent.pathId
        Row {
            Text("Path: ")
            LinkText(pathName, onClick = { onLinkClick(linkKeyForPath(talent.pathId)) })
        }
        talent.specialty?.let { Text("Specialty: $it") }
        talent.focusCost?.let { Text("Focus Cost: $it") }

        val otherPrerequisites = buildList {
            if (talent.prerequisiteSkills.isNotEmpty()) {
                add(
                    "Skills: " + talent.prerequisiteSkills.entries.joinToString {
                        "${formatIdentifier(it.key)} ${it.value}"
                    },
                )
            }
            talent.prerequisiteLevel?.let { add("Level: $it") }
            talent.prerequisiteIdealSpoken?.let { add("Requires Ideal $it spoken") }
            talent.prerequisiteOther?.let { add(it) }
        }

        if (otherPrerequisites.isEmpty() && talent.prerequisiteTalents.isEmpty()) {
            Text("Prerequisites: None")
        } else {
            Text("Prerequisites:")
            otherPrerequisites.forEach { Text("• $it") }
            if (talent.prerequisiteTalents.isNotEmpty()) {
                val mode = if (talent.prerequisiteTalentsMode.equals("any", ignoreCase = true)) {
                    "any of"
                } else {
                    "all of"
                }
                Text("• Talents ($mode):")
                talent.prerequisiteTalents.forEach { prereqId ->
                    val prereqName = RulesRepository.talents.firstOrNull { t -> t.id == prereqId }?.name ?: prereqId
                    LinkText(
                        "◦ $prereqName",
                        onClick = { onLinkClick(talentReferenceKey(prereqId)) },
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SurgeDetails(surge: SurgeEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Attribute: ${formatIdentifier(surge.attributeId)}")
        if (surge.radiantOrders.isNotEmpty()) {
            Text("Radiant Orders: ${surge.radiantOrders.joinToString { formatIdentifier(it) }}")
        }
        if (surge.talents.isNotEmpty()) {
            Text("Talents:")
            surge.talents.forEach { surgeTalent -> SurgeTalentRow(surgeTalent) }
        }
    }
}

@Composable
private fun SurgeTalentRow(surgeTalent: SurgeTalent) {
    Column(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
        Text(
            text = "• ${surgeTalent.name} (${formatActivation(surgeTalent.activationType)})",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = surgeTalent.summary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

private val SurgeTalent.activationType: Activation
    get() = when (activation.lowercase()) {
        "action1" -> Activation.ACTION1
        "action2" -> Activation.ACTION2
        "action3" -> Activation.ACTION3
        "free" -> Activation.FREE
        "reaction" -> Activation.REACTION
        "special" -> Activation.SPECIAL
        else -> Activation.PASSIVE
    }

@Composable
private fun ConditionDetails(condition: Condition) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Has bracketed value: ${if (condition.hasBracketValue) "Yes" else "No"}")
        Text("Cumulative: ${if (condition.cumulative) "Yes" else "No"}")
    }
}
