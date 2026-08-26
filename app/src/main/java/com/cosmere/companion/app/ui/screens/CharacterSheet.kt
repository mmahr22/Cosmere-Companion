package com.cosmere.companion.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cosmere.companion.app.data.saveAvatar
import com.cosmere.companion.core.data.RulesRepository
import com.cosmere.companion.core.model.Attribute
import com.cosmere.companion.core.model.Defense
import com.cosmere.companion.core.model.PlayerCharacter
import com.cosmere.companion.core.model.Skill
import kotlinx.coroutines.launch

private enum class SheetTab(val label: String) {
    MAIN("Main"),
    SKILLS("Skills"),
    TALENTS("Talents"),
    INVENTORY("Inventory"),
    NOTES("Notes"),
    GM("GM"),
}

@Composable
internal fun CharacterSheet(
    character: PlayerCharacter,
    onUpdate: (PlayerCharacter) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onOpenReference: (String) -> Unit,
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
    val equippedArmor = remember(character.equippedArmorId) {
        character.equippedArmorId?.let { RulesRepository.itemById(it) }
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val path = saveAvatar(context, character.id, uri, character.avatarPath)
                onUpdate(character.copy(avatarPath = path))
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { SheetTab.entries.size })

    Column(modifier = Modifier.fillMaxSize()) {
      Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to characters")
            }
            Box {
                CharacterAvatar(
                    avatarPath = character.avatarPath,
                    name = character.name,
                    size = 56.dp,
                    modifier = Modifier.clickable {
                        pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        contentDescription = "Change avatar",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(character.name, style = MaterialTheme.typography.headlineSmall)
        }
        if (ancestry != null || cultures.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ancestry?.let { a ->
                    SheetLink(a.name, onClick = { onOpenReference(ancestryReferenceKey(a.id)) })
                    if (cultures.isNotEmpty()) Text(" · ", style = MaterialTheme.typography.bodyMedium)
                }
                cultures.forEachIndexed { index, culture ->
                    SheetLink(culture.name, onClick = { onOpenReference(cultureReferenceKey(culture.id)) })
                    if (index != cultures.lastIndex) Text(" · ", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        // One row per accessible heroic path (see PlayerCharacter.accessiblePathIds), ordered by
        // when each path's key talent was first purchased — the starting path is always first.
        val heroicPathSummaries = remember(character.purchasedTalentIds, character.heroicPathId, character.specialty) {
            val purchasedTalents = character.purchasedTalentIds.mapNotNull { RulesRepository.talentById(it) }
            val firstIndexByPath = mutableMapOf<String, Int>()
            purchasedTalents.forEachIndexed { index, talent -> firstIndexByPath.getOrPut(talent.pathId) { index } }
            character.accessiblePathIds
                .mapNotNull { RulesRepository.pathById(it) }
                .filter { it.type == "heroic" }
                .sortedBy { firstIndexByPath[it.id] ?: Int.MAX_VALUE }
                .map { path ->
                    val talentsInPath = purchasedTalents.filter { it.pathId == path.id }
                    val specialty = if (path.id == character.heroicPathId) {
                        character.specialty
                    } else {
                        talentsInPath.mapNotNull { it.specialty }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
                    }
                    Triple(path, specialty, talentsInPath.size)
                }
        }
        heroicPathSummaries.forEach { (path, specialty, count) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                SheetLink(path.name, onClick = { onOpenReference(pathReferenceKey(path.id)) })
                val suffix = (specialty?.let { " ($it)" } ?: "") + " - $count"
                Text(suffix, style = MaterialTheme.typography.bodyMedium)
            }
        }
        radiantPath?.let { rp ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                SheetLink(rp.name, onClick = { onOpenReference(pathReferenceKey(rp.id)) })
                Text(
                    " · Bonded to ${rp.sprenType ?: "a spren"} · ${idealOrdinal(character.spokenIdeal)} Ideal",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        var showLevelUpDialog by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Level ${character.level}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { showLevelUpDialog = true },
            )
            if (character.level < 20) {
                Button(
                    onClick = {
                        onUpdate(character.copy(level = character.level + 1))
                        showLevelUpDialog = true
                    },
                ) {
                    Text("Level Up")
                }
            }
        }
        if (showLevelUpDialog) {
            LevelUpDialog(
                character = character,
                heroicPath = heroicPath,
                radiantPath = radiantPath,
                onUpdate = onUpdate,
                onOpenReference = onOpenReference,
                onDismiss = { showLevelUpDialog = false },
            )
        }

        var editingResource by remember { mutableStateOf<ResourceKind?>(null) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResourcePill(
                label = "Health",
                current = character.currentHealth,
                max = character.maxHealth,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                onClick = { editingResource = ResourceKind.HEALTH },
                modifier = Modifier.weight(1f),
            )
            ResourcePill(
                label = "Focus",
                current = character.currentFocus,
                max = character.maxFocus,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                onClick = { editingResource = ResourceKind.FOCUS },
                modifier = Modifier.weight(1f),
            )
            ResourcePill(
                label = "Investiture",
                current = character.currentInvestiture,
                max = character.maxInvestiture,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = { editingResource = ResourceKind.INVESTITURE },
                modifier = Modifier.weight(1f),
            )
        }
        when (editingResource) {
            ResourceKind.HEALTH -> ResourceEditorDialog(
                label = "Health",
                current = character.currentHealth,
                max = character.maxHealth,
                onCurrentChange = { onUpdate(character.copy(currentHealth = it.coerceIn(0, character.maxHealth))) },
                onMaxChange = null,
                onDismiss = { editingResource = null },
            )
            ResourceKind.FOCUS -> ResourceEditorDialog(
                label = "Focus",
                current = character.currentFocus,
                max = character.maxFocus,
                onCurrentChange = { onUpdate(character.copy(currentFocus = it.coerceIn(0, character.maxFocus))) },
                onMaxChange = null,
                onDismiss = { editingResource = null },
            )
            ResourceKind.INVESTITURE -> ResourceEditorDialog(
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
                onDismiss = { editingResource = null },
            )
            null -> {}
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Defense.entries.forEach { defense ->
                Box(modifier = Modifier.weight(1f)) {
                    val attributes = Attribute.entries.filter { it.defense == defense }
                        .map { it to character.effectiveAttribute(it) }
                    StatBadge(
                        label = defense.displayName,
                        value = character.defense(defense),
                        tooltipText = "10 + " + attributes.joinToString(" + ") { (attribute, value) -> "${attribute.displayName} ($value)" },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                StatBadge(
                    label = "Deflect",
                    value = character.deflectValue,
                    tooltipText = equippedArmor?.let { "${it.name}: Deflect ${it.deflectValue ?: 0}" } ?: "No armor equipped",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

      }

      ScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
          SheetTab.entries.forEach { tab ->
              Tab(
                  selected = pagerState.currentPage == tab.ordinal,
                  onClick = { coroutineScope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
                  text = { Text(tab.label) },
              )
          }
      }

      HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
      Column(
          modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        when (SheetTab.entries[page]) {
            SheetTab.MAIN -> {
                if (character.ancestryId == "singer") {
                    SingerFormsSection(character = character, onUpdate = onUpdate)
                    HorizontalDivider()
                }

                ConditionsSection(character = character, onUpdate = onUpdate, onOpenReference = onOpenReference)

                HorizontalDivider()

                Text("Attributes", style = MaterialTheme.typography.titleMedium)
                Attribute.entries.forEach { attribute ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${attribute.displayName} (${attribute.abbreviation})")
                        Text("${character.attribute(attribute)}", fontWeight = FontWeight.Bold)
                    }
                }
            }

            SheetTab.SKILLS -> {
                Text("Skills", style = MaterialTheme.typography.titleMedium)
                Attribute.entries.forEach { attribute ->
                    Text(attribute.displayName, style = MaterialTheme.typography.labelLarge)
                    Skill.forAttribute(attribute).forEach { skill ->
                        val autoMinimum = if (skill.name == heroicPath?.startingSkillId) 1 else 0
                        val rank = character.skillRank(skill.name)
                        val modifierValue = character.effectiveAttribute(attribute) + rank
                        var rollState by remember(skill) { mutableStateOf<SkillRollUiState>(SkillRollUiState.Collapsed) }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(skill.displayName + if (autoMinimum > 0) " (path)" else "")
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SkillRollTrigger(
                                        label = skill.displayName,
                                        modifierValue = modifierValue,
                                        onStateChange = { rollState = it },
                                    )
                                    Text(
                                        "$rank",
                                        modifier = Modifier.width(24.dp),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            SkillRollExpansion(
                                modifierValue = modifierValue,
                                state = rollState,
                                onStateChange = { rollState = it },
                            )
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(surgeName)
                            Text("$rank", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            SheetTab.TALENTS -> {
                Text("Talents", style = MaterialTheme.typography.titleMedium)
                val purchasedTalents = remember(character.purchasedTalentIds) {
                    character.purchasedTalentIds.mapNotNull { RulesRepository.talentById(it) }.sortedBy { it.name }
                }
                var detailTalentId by remember { mutableStateOf<String?>(null) }
                if (purchasedTalents.isEmpty()) {
                    Text(
                        "No talents yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    purchasedTalents.forEach { talent ->
                        Column {
                            SheetLink(talent.name, onClick = { detailTalentId = talent.id })
                            Text(
                                talent.specialty ?: (RulesRepository.pathById(talent.pathId)?.name ?: talent.pathId),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                detailTalentId?.let { id ->
                    TalentDetailDialog(talentId = id, onDismiss = { detailTalentId = null })
                }
            }

            SheetTab.INVENTORY -> InventorySection(character = character, onUpdate = onUpdate)

            SheetTab.NOTES -> {
                Text("Notes", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = character.notes,
                    onValueChange = { onUpdate(character.copy(notes = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Backstory, appearance, session notes…") },
                    minLines = 4,
                )

                var showDeleteConfirm by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete Character")
                }
                if (showDeleteConfirm) {
                    DeleteCharacterDialog(
                        characterName = character.name,
                        onConfirm = {
                            showDeleteConfirm = false
                            onDelete()
                        },
                        onDismiss = { showDeleteConfirm = false },
                    )
                }
            }

            SheetTab.GM -> {
                Text("GM Overrides", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Manually grant bonus points beyond the normal level budget. Spend them from the " +
                        "Level Up screen (tap \"Level ${character.level}\" above).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GmBonusRow(
                    label = "Attribute Points",
                    value = character.bonusAttributePoints,
                    onChange = { onUpdate(character.copy(bonusAttributePoints = it)) },
                )
                GmBonusRow(
                    label = "Skill Points",
                    value = character.bonusSkillPoints,
                    onChange = { onUpdate(character.copy(bonusSkillPoints = it)) },
                )
                GmBonusRow(
                    label = "Talent Points",
                    value = character.bonusTalentPoints,
                    onChange = { onUpdate(character.copy(bonusTalentPoints = it)) },
                )
            }
        }
      }
      }
    }
}

private fun idealOrdinal(ideal: Int): String = when (ideal) {
    1 -> "First"
    2 -> "Second"
    3 -> "Third"
    4 -> "Fourth"
    else -> "No"
}

/** A small stepper for ad-hoc GM-granted points, shown on the GM tab or under a budgeted section's header in the Level Up screen. */
@Composable
private fun GmBonusRow(label: String = "GM Bonus", value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(
            onClick = { if (value > 0) onChange(value - 1) },
            enabled = value > 0,
        ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease $label") }
        Text("$value", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onChange(value + 1) }) {
            Icon(Icons.Filled.Add, contentDescription = "Increase $label")
        }
    }
}

private enum class ResourceKind { HEALTH, FOCUS, INVESTITURE }

/** A compact badge for a resource (Health/Focus/Investiture) shown in the sheet header. Tap to adjust. */
@Composable
private fun ResourcePill(
    label: String,
    current: Int,
    max: Int,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "$current/$max",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

/** A compact derived-stat badge in the sheet header. Tap shows a tooltip explaining what makes up the value. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatBadge(label: String, value: Int, tooltipText: String, modifier: Modifier = Modifier) {
    val tooltipState = rememberTooltipState()
    val coroutineScope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltipText) } },
        state = tooltipState,
        enableUserInput = false,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { coroutineScope.launch { tooltipState.show() } }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "$value",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResourceEditorDialog(
    label: String,
    current: Int,
    max: Int,
    onCurrentChange: (Int) -> Unit,
    onMaxChange: ((Int) -> Unit)?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ResourceNumberField(
                            value = current,
                            onValueChange = onCurrentChange,
                            contentDescription = label,
                        )
                        Text(" / $max", style = MaterialTheme.typography.titleLarge)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (current > 0) onCurrentChange(current - 1) },
                            enabled = current > 0,
                        ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease $label") }
                        IconButton(
                            onClick = { if (current < max) onCurrentChange(current + 1) },
                            enabled = current < max,
                        ) { Icon(Icons.Filled.Add, contentDescription = "Increase $label") }
                    }
                }
                if (onMaxChange != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Max ", style = MaterialTheme.typography.bodyMedium)
                            ResourceNumberField(
                                value = max,
                                onValueChange = onMaxChange,
                                contentDescription = "max $label",
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

/** A narrow numeric text field, used inside [ResourceEditorDialog] so values can be typed directly instead of only stepped. */
@Composable
private fun ResourceNumberField(value: Int, onValueChange: (Int) -> Unit, contentDescription: String) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val digitsOnly = input.filter { it.isDigit() }
            text = digitsOnly
            digitsOnly.toIntOrNull()?.let(onValueChange)
        },
        modifier = Modifier
            .width(72.dp)
            .semantics { this.contentDescription = contentDescription },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
