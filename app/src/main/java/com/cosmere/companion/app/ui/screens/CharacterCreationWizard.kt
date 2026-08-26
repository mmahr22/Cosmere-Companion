package com.cosmere.companion.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cosmere.companion.core.data.RulesRepository
import com.cosmere.companion.core.model.Ancestry
import com.cosmere.companion.core.model.Attribute
import com.cosmere.companion.core.model.CharacterMath
import com.cosmere.companion.core.model.Defense
import com.cosmere.companion.core.model.GamePath
import com.cosmere.companion.core.model.PlayerCharacter
import com.cosmere.companion.core.model.Skill
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val MAX_CULTURES = 2

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

private enum class CreationStep(val title: String) {
    ANCESTRY("Ancestry"),
    ATTRIBUTES("Attributes"),
    PATH("Heroic Path"),
    SKILLS("Skills"),
    RADIANT("Radiant"),
    REVIEW("Review"),
}

@Composable
internal fun CharacterCreationForm(onCreate: (PlayerCharacter) -> Unit, onCancel: () -> Unit) {
    val heroicPaths = remember { RulesRepository.paths.filter { it.type == "heroic" } }
    val radiantPaths = remember { RulesRepository.paths.filter { it.type == "radiant" } }

    var draft by rememberSaveable(stateSaver = CharacterDraftSaver) { mutableStateOf(CharacterDraft()) }
    var stepIndex by rememberSaveable { mutableStateOf(0) }

    val heroicPath = heroicPaths.firstOrNull { it.id == draft.heroicPathId }
    val radiantPath = radiantPaths.firstOrNull { it.id == draft.radiantPathId }
    val creationSkillCap = CharacterMath.maxSkillRank(1)

    val attributePointsRemaining = CharacterMath.CREATION_ATTRIBUTE_POINTS - draft.attributes.values.sum()
    val skillPointsRemaining = CharacterMath.CREATION_FREE_SKILL_RANKS - Skill.entries.sumOf { skill ->
        val autoMinimum = if (skill.name == heroicPath?.startingSkillId) 1 else 0
        (draft.skillRank(skill.name) - autoMinimum).coerceAtLeast(0)
    }

    fun errorFor(step: CreationStep): String? = when (step) {
        CreationStep.ANCESTRY -> when {
            draft.name.isBlank() -> "Enter a character name"
            draft.ancestryId == null -> "Choose an ancestry"
            else -> null
        }
        CreationStep.ATTRIBUTES -> when {
            attributePointsRemaining > 0 ->
                "Assign $attributePointsRemaining more attribute point${if (attributePointsRemaining == 1) "" else "s"}"
            attributePointsRemaining < 0 ->
                "Remove ${-attributePointsRemaining} attribute point${if (attributePointsRemaining == -1) "" else "s"}"
            else -> null
        }
        CreationStep.PATH -> when {
            draft.heroicPathId == null -> "Choose a Heroic Path"
            heroicPath?.specialties?.isNotEmpty() == true && draft.specialty == null -> "Choose a specialty"
            else -> null
        }
        CreationStep.SKILLS -> when {
            skillPointsRemaining > 0 -> "Assign $skillPointsRemaining more skill rank${if (skillPointsRemaining == 1) "" else "s"}"
            skillPointsRemaining < 0 -> "Remove ${-skillPointsRemaining} skill rank${if (skillPointsRemaining == -1) "" else "s"}"
            else -> null
        }
        CreationStep.RADIANT -> if (draft.isRadiant && draft.radiantPathId == null) "Choose a Radiant Order" else null
        CreationStep.REVIEW -> null
    }

    val steps = CreationStep.entries
    val currentStep = steps[stepIndex]
    val canCreate = steps.dropLast(1).all { errorFor(it) == null }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel and return to characters")
                }
                Text("Create Your Character", style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            val remainingSuffix = when (currentStep) {
                CreationStep.ATTRIBUTES ->
                    " · $attributePointsRemaining point${if (attributePointsRemaining == 1) "" else "s"} remaining"
                CreationStep.SKILLS -> " · $skillPointsRemaining rank${if (skillPointsRemaining == 1) "" else "s"} remaining"
                else -> ""
            }
            Text(
                "Step ${stepIndex + 1} of ${steps.size}: ${currentStep.title}$remainingSuffix",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (remainingSuffix.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
            )
            LinearProgressIndicator(
                progress = { (stepIndex + 1f) / steps.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }

        val stepScrollState = rememberScrollState()
        LaunchedEffect(currentStep, heroicPath?.id) {
            if (currentStep == CreationStep.PATH && heroicPath?.specialties?.isNotEmpty() == true) {
                stepScrollState.animateScrollTo(stepScrollState.maxValue)
            } else {
                stepScrollState.scrollTo(0)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(stepScrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (currentStep) {
                CreationStep.ANCESTRY -> AncestryStep(draft = draft, onChange = { draft = it })
                CreationStep.ATTRIBUTES -> AttributesStep(
                    draft = draft,
                    attributePointsRemaining = attributePointsRemaining,
                    onChange = { draft = it },
                )
                CreationStep.PATH -> PathStep(
                    draft = draft,
                    heroicPaths = heroicPaths,
                    heroicPath = heroicPath,
                    onChange = { draft = it },
                )
                CreationStep.SKILLS -> SkillsStep(
                    draft = draft,
                    heroicPath = heroicPath,
                    skillPointsRemaining = skillPointsRemaining,
                    creationSkillCap = creationSkillCap,
                    onChange = { draft = it },
                )
                CreationStep.RADIANT -> RadiantStep(
                    draft = draft,
                    radiantPaths = radiantPaths,
                    radiantPath = radiantPath,
                    onChange = { draft = it },
                )
                CreationStep.REVIEW -> ReviewStep(draft = draft, heroicPath = heroicPath, radiantPath = radiantPath)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        HorizontalDivider()

        Column(modifier = Modifier.padding(16.dp)) {
            val error = errorFor(currentStep)
            if (error != null) {
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (stepIndex > 0) {
                    OutlinedButton(onClick = { stepIndex-- }, modifier = Modifier.weight(1f)) {
                        Text("Back")
                    }
                }
                if (currentStep == CreationStep.REVIEW) {
                    Button(
                        onClick = {
                            val ancestry = draft.ancestryId?.let { RulesRepository.ancestryById(it) }
                            onCreate(
                                PlayerCharacter(
                                    name = draft.name.trim(),
                                    ancestryId = draft.ancestryId,
                                    cultureIds = draft.cultureIds,
                                    attributes = Attribute.entries.associateWith { draft.attributeValue(it) },
                                    heroicPathId = requireNotNull(draft.heroicPathId) {
                                        "heroic path must be chosen before Create is reachable"
                                    },
                                    specialty = draft.specialty,
                                    radiantPathId = draft.radiantPathId,
                                    skillRanks = draft.skillRanks,
                                    purchasedTalentIds = listOfNotNull(
                                        heroicPath?.keyTalentId,
                                        radiantPath?.keyTalentId,
                                        ancestry?.keyTalentId,
                                    ),
                                ),
                            )
                        },
                        enabled = canCreate,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Create Character")
                    }
                } else {
                    Button(
                        onClick = { stepIndex++ },
                        enabled = error == null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Next")
                    }
                }
            }
        }
    }
}

@Composable
private fun AncestryStep(draft: CharacterDraft, onChange: (CharacterDraft) -> Unit) {
    OutlinedTextField(
        value = draft.name,
        onValueChange = { onChange(draft.copy(name = it)) },
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
                onChange(
                    draft.copy(
                        ancestryId = ancestry.id,
                        cultureIds = if (ancestry.id != "singer") draft.cultureIds - "listener" else draft.cultureIds,
                    ),
                )
            },
        )
    }

    Text(
        "Culture (choose up to $MAX_CULTURES)",
        style = MaterialTheme.typography.titleMedium,
    )

    var cultureListExpanded by remember { mutableStateOf(false) }
    var expandedCultureId by remember { mutableStateOf<String?>(null) }

    val availableCultures = remember(draft.ancestryId) {
        RulesRepository.cultures.filter { !it.singerOnly || draft.ancestryId == "singer" }
    }
    val selectedCultureNames = remember(draft.cultureIds) {
        draft.cultureIds.mapNotNull { id -> RulesRepository.cultures.firstOrNull { it.id == id }?.name }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { cultureListExpanded = !cultureListExpanded },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (selectedCultureNames.isEmpty()) "Select cultures" else selectedCultureNames.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
            )
            Icon(
                imageVector = if (cultureListExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (cultureListExpanded) "Collapse culture list" else "Expand culture list",
            )
        }
    }

    if (cultureListExpanded) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            availableCultures.forEach { culture ->
                val selected = culture.id in draft.cultureIds
                val infoExpanded = expandedCultureId == culture.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedCultureId = if (infoExpanded) null else culture.id },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = {
                                        onChange(
                                            when {
                                                selected -> draft.copy(cultureIds = draft.cultureIds - culture.id)
                                                draft.cultureIds.size < MAX_CULTURES ->
                                                    draft.copy(cultureIds = draft.cultureIds + culture.id)
                                                else -> draft
                                            },
                                        )
                                    },
                                )
                                Text(
                                    culture.name,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                            Icon(
                                imageVector = if (infoExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (infoExpanded) "Collapse" else "Expand",
                            )
                        }
                        if (infoExpanded) {
                            Text(
                                culture.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Features: ") }
                                    append(culture.expertiseSummary)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttributesStep(
    draft: CharacterDraft,
    attributePointsRemaining: Int,
    onChange: (CharacterDraft) -> Unit,
) {
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
                        if (value > 0) onChange(draft.copy(attributes = draft.attributes + (attribute.name to value - 1)))
                    },
                    enabled = value > 0,
                ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease ${attribute.displayName}") }
                Text("$value", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                IconButton(
                    onClick = {
                        if (value < CharacterMath.CREATION_ATTRIBUTE_MAX && attributePointsRemaining > 0) {
                            onChange(draft.copy(attributes = draft.attributes + (attribute.name to value + 1)))
                        }
                    },
                    enabled = value < CharacterMath.CREATION_ATTRIBUTE_MAX && attributePointsRemaining > 0,
                ) { Icon(Icons.Filled.Add, contentDescription = "Increase ${attribute.displayName}") }
            }
        }
    }

    HorizontalDivider()
    AttributeStatsPreview(draft)
}

@Composable
private fun AttributeStatsPreview(draft: CharacterDraft) {
    val attributes = remember(draft.attributes) { Attribute.entries.associateWith { draft.attributeValue(it) } }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "Health ${CharacterMath.maxHealth(1, attributes[Attribute.STRENGTH] ?: 0)} · " +
                    "Focus ${CharacterMath.maxFocus(attributes[Attribute.WILLPOWER] ?: 0)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Defense.entries.forEach { defense ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(defense.displayName, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${CharacterMath.defense(defense, attributes)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PathStep(
    draft: CharacterDraft,
    heroicPaths: List<GamePath>,
    heroicPath: GamePath?,
    onChange: (CharacterDraft) -> Unit,
) {
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
                onChange(draft.copy(heroicPathId = path.id, specialty = null, skillRanks = skillRanks))
            },
        )
    }
    if (heroicPath != null && heroicPath.specialties.isNotEmpty()) {
        Text("Specialty", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            heroicPath.specialties.forEach { spec ->
                FilterChip(
                    selected = draft.specialty == spec,
                    onClick = { onChange(draft.copy(specialty = spec)) },
                    label = { Text(spec) },
                )
            }
        }
    }
}

@Composable
private fun SkillsStep(
    draft: CharacterDraft,
    heroicPath: GamePath?,
    skillPointsRemaining: Int,
    creationSkillCap: Int,
    onChange: (CharacterDraft) -> Unit,
) {
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
                            if (rank > autoMinimum) onChange(draft.withSkillRank(skill.name, rank - 1))
                        },
                        enabled = rank > autoMinimum,
                    ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease ${skill.displayName}") }
                    Text("$rank", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    IconButton(
                        onClick = {
                            if (rank < creationSkillCap && skillPointsRemaining > 0) {
                                onChange(draft.withSkillRank(skill.name, rank + 1))
                            }
                        },
                        enabled = rank < creationSkillCap && skillPointsRemaining > 0,
                    ) { Icon(Icons.Filled.Add, contentDescription = "Increase ${skill.displayName}") }
                }
            }
        }
    }
}

@Composable
private fun RadiantStep(
    draft: CharacterDraft,
    radiantPaths: List<GamePath>,
    radiantPath: GamePath?,
    onChange: (CharacterDraft) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(
            checked = draft.isRadiant,
            onCheckedChange = { checked ->
                onChange(
                    if (checked) {
                        draft.copy(isRadiant = true)
                    } else {
                        var skillRanks = draft.skillRanks
                        radiantPath?.surgeIds?.forEach { surgeId ->
                            val current = skillRanks[surgeId] ?: 0
                            skillRanks = if (current <= 1) skillRanks - surgeId else skillRanks + (surgeId to current - 1)
                        }
                        draft.copy(isRadiant = false, radiantPathId = null, skillRanks = skillRanks)
                    },
                )
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
                    onChange(draft.copy(radiantPathId = path.id, skillRanks = skillRanks))
                },
            )
        }
    } else {
        Text(
            "Optional — enable this only if your character begins play already bonded to a spren.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReviewStep(draft: CharacterDraft, heroicPath: GamePath?, radiantPath: GamePath?) {
    val ancestry = remember(draft.ancestryId) { draft.ancestryId?.let(RulesRepository::ancestryById) }
    val cultures = remember(draft.cultureIds) { draft.cultureIds.mapNotNull(RulesRepository::cultureById) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                draft.name.ifBlank { "(unnamed)" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                (listOfNotNull(ancestry?.name) + cultures.map { it.name })
                    .joinToString(" · ")
                    .ifBlank { "No ancestry chosen" },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                buildString {
                    append(heroicPath?.name ?: "No Heroic Path chosen")
                    draft.specialty?.let { append(" — $it") }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (draft.isRadiant) {
                Text(
                    radiantPath?.let { "${it.name} · Bonded to ${it.sprenType ?: "a spren"}" } ?: "Radiant (no order chosen)",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    Text("Attributes", style = MaterialTheme.typography.labelLarge)
    Attribute.entries.forEach { attribute ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${attribute.displayName} (${attribute.abbreviation})")
            Text("${draft.attributeValue(attribute)}", fontWeight = FontWeight.Bold)
        }
    }

    AttributeStatsPreview(draft)

    val skillEntries = Skill.entries.filter { draft.skillRank(it.name) > 0 }
    if (skillEntries.isNotEmpty()) {
        Text("Skills", style = MaterialTheme.typography.labelLarge)
        skillEntries.forEach { skill ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(skill.displayName)
                Text("${draft.skillRank(skill.name)}", fontWeight = FontWeight.Bold)
            }
        }
    }

    val surgeEntries = draft.skillRanks.keys.filter { key -> Skill.entries.none { it.name == key } && draft.skillRank(key) > 0 }
    if (surgeEntries.isNotEmpty()) {
        Text("Surges", style = MaterialTheme.typography.labelLarge)
        surgeEntries.forEach { surgeId ->
            val surgeName = RulesRepository.surgeById(surgeId)?.name ?: surgeId
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(surgeName)
                Text("${draft.skillRank(surgeId)}", fontWeight = FontWeight.Bold)
            }
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
                maxLines = if (selected) Int.MAX_VALUE else 2,
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
