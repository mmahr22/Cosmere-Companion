package com.cosmere.companion.app.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cosmere.companion.app.data.readCharacterExport
import com.cosmere.companion.app.data.saveAvatar
import com.cosmere.companion.app.data.writeCharacterExport
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
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val MAX_CULTURES = 2

@Composable
fun CharactersScreen(onOpenReference: (String) -> Unit = {}, viewModel: CharacterViewModel = viewModel()) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()

    // 0 means "no character open" — real ids start at 1 (Room autoGenerate).
    var openCharacterId by rememberSaveable { mutableStateOf(0) }
    var isCreating by rememberSaveable { mutableStateOf(false) }
    val openCharacter = characters.firstOrNull { it.id == openCharacterId }

    when {
        isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        isCreating -> CharacterCreationForm(
            onCreate = { character ->
                viewModel.save(character)
                isCreating = false
            },
        )
        openCharacter != null -> CharacterSheet(
            character = openCharacter,
            onUpdate = viewModel::save,
            onBack = { openCharacterId = 0 },
            onDelete = {
                viewModel.delete(openCharacter)
                openCharacterId = 0
            },
            onOpenReference = onOpenReference,
        )
        else -> CharacterRosterScreen(
            characters = characters,
            onSelect = { openCharacterId = it.id },
            onCreateNew = { isCreating = true },
            onDelete = viewModel::delete,
            onImport = viewModel::save,
        )
    }
}

@Composable
private fun CharacterRosterScreen(
    characters: List<PlayerCharacter>,
    onSelect: (PlayerCharacter) -> Unit,
    onCreateNew: () -> Unit,
    onDelete: (PlayerCharacter) -> Unit,
    onImport: (PlayerCharacter) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<PlayerCharacter?>(null) }
    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val imported = readCharacterExport(context, uri)
            onImport(imported)
            Toast.makeText(context, "Imported ${imported.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Couldn't read that file as a character export.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Characters", style = MaterialTheme.typography.headlineSmall)
            Row {
                IconButton(onClick = { importLauncher.launch("*/*") }) {
                    Icon(Icons.Filled.FileOpen, contentDescription = "Import character")
                }
                IconButton(onClick = onCreateNew) {
                    Icon(Icons.Filled.Add, contentDescription = "Create character")
                }
            }
        }

        if (characters.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "No characters yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onCreateNew) {
                    Text("Create Character")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(characters, key = { it.id }) { character ->
                    CharacterRosterRow(
                        character = character,
                        onClick = { onSelect(character) },
                        onDeleteRequest = { pendingDelete = character },
                    )
                }
            }
        }
    }

    pendingDelete?.let { character ->
        DeleteCharacterDialog(
            characterName = character.name,
            onConfirm = {
                onDelete(character)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun CharacterRosterRow(
    character: PlayerCharacter,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val ancestry = remember(character.ancestryId) { character.ancestryId?.let(RulesRepository::ancestryById) }
    val heroicPath = remember(character.heroicPathId) { RulesRepository.pathById(character.heroicPathId) }
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CharacterAvatar(avatarPath = character.avatarPath, name = character.name, size = 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(character.name, style = MaterialTheme.typography.titleMedium)
                val subtitle = buildString {
                    ancestry?.let { append(it.name) }
                    heroicPath?.let { path ->
                        if (isNotEmpty()) append(" · ")
                        append(path.name)
                        character.specialty?.let { append(" — $it") }
                    }
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                "Lv ${character.level} · ${character.currentHealth}/${character.maxHealth} HP",
                style = MaterialTheme.typography.bodySmall,
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options for ${character.name}")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Export") },
                        onClick = {
                            menuExpanded = false
                            val uri = writeCharacterExport(context, character)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export ${character.name}"))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            onDeleteRequest()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteCharacterDialog(characterName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete $characterName?") },
        text = { Text("This can't be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private val AVATAR_COLORS = listOf(
    Color(0xFF6750A4), Color(0xFF386A20), Color(0xFFB3261E), Color(0xFF0061A4), Color(0xFF8B5000),
)

private fun avatarColorFor(name: String): Color = AVATAR_COLORS[(name.hashCode() and 0x7FFFFFFF) % AVATAR_COLORS.size]

private fun initialsFor(name: String): String =
    name.trim().split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

private fun pointsSuffix(remaining: Int): String = when {
    remaining > 0 -> " ($remaining remaining)"
    remaining < 0 -> " (${-remaining} over)"
    else -> ""
}

/** A tappable name that jumps to the matching entry in the Reference tab. */
@Composable
private fun SheetLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** A small stepper for ad-hoc GM-granted points, shown under a budgeted section's header. */
@Composable
private fun GmBonusRow(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "GM Bonus",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(
            onClick = { if (value > 0) onChange(value - 1) },
            enabled = value > 0,
        ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease GM bonus") }
        Text("$value", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onChange(value + 1) }) {
            Icon(Icons.Filled.Add, contentDescription = "Increase GM bonus")
        }
    }
}

@Composable
private fun CharacterAvatar(
    avatarPath: String?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val file = remember(avatarPath) { avatarPath?.let(::File) }
    if (file != null && file.exists()) {
        AsyncImage(
            model = file,
            contentDescription = "$name's avatar",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(avatarColorFor(name)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initialsFor(name),
                color = Color.White,
                style = if (size > 40.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
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

private enum class CreationStep(val title: String) {
    ANCESTRY("Ancestry"),
    ATTRIBUTES("Attributes"),
    PATH("Heroic Path"),
    SKILLS("Skills"),
    RADIANT("Radiant"),
    REVIEW("Review"),
}

@Composable
private fun CharacterCreationForm(onCreate: (PlayerCharacter) -> Unit) {
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
            Text("Create Your Character", style = MaterialTheme.typography.headlineSmall)
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

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
    SpecialtyPicker(
        specialties = heroicPath?.specialties.orEmpty(),
        selected = draft.specialty,
        onSelect = { spec -> onChange(draft.copy(specialty = spec)) },
    )
}

@Composable
private fun SpecialtyPicker(specialties: List<String>, selected: String?, onSelect: (String) -> Unit) {
    if (specialties.isEmpty()) return
    Text("Specialty", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        specialties.forEach { spec ->
            SpecialtyChip(
                label = spec,
                selected = selected == spec,
                onClick = { onSelect(spec) },
            )
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

/**
 * A tappable pill matching [FilterChip]'s look, used in place of the real
 * thing for the specialty picker — Material3's `FilterChip` was silently
 * failing to render or receive taps here (though it renders fine elsewhere
 * in the app), while this plain `Card` uses the exact pattern already proven
 * reliable for [SelectablePathRow] and the culture picker's rows.
 */
@Composable
private fun SpecialtyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun CharacterSheet(
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

    val context = LocalContext.current
    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val path = saveAvatar(context, character.id, uri, character.avatarPath)
            onUpdate(character.copy(avatarPath = path))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (heroicPath != null) {
                SheetLink(heroicPath.name, onClick = { onOpenReference(pathReferenceKey(heroicPath.id)) })
            } else {
                Text(character.heroicPathId, style = MaterialTheme.typography.bodyMedium)
            }
            character.specialty?.let { Text(" — $it", style = MaterialTheme.typography.bodyMedium) }
        }
        radiantPath?.let { rp ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                SheetLink(rp.name, onClick = { onOpenReference(pathReferenceKey(rp.id)) })
                Text(" · Bonded to ${rp.sprenType ?: "a spren"}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Level ${character.level}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (character.level < 20) {
                Button(onClick = { onUpdate(character.copy(level = character.level + 1)) }) {
                    Text("Level Up")
                }
            }
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

        Text(
            "Attributes" + pointsSuffix(attributePointsRemaining),
            style = MaterialTheme.typography.titleMedium,
        )
        GmBonusRow(
            value = character.bonusAttributePoints,
            onChange = { onUpdate(character.copy(bonusAttributePoints = it)) },
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

        Text(
            "Skills" + pointsSuffix(skillPointsRemaining),
            style = MaterialTheme.typography.titleMedium,
        )
        GmBonusRow(
            value = character.bonusSkillPoints,
            onChange = { onUpdate(character.copy(bonusSkillPoints = it)) },
        )
        val skillCap = CharacterMath.maxSkillRank(character.level)
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
        InventorySection(character = character, onUpdate = onUpdate)

        HorizontalDivider()
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
            if (equippedWeapons.isEmpty()) {
                Text("Weapons: None", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Weapons:", style = MaterialTheme.typography.bodySmall)
                equippedWeapons.forEach { weapon ->
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(weapon.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        weaponQuickStats(weapon)?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
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
