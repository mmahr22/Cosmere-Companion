package com.cosmere.companion.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cosmere.companion.core.dice.DamageRollResult
import com.cosmere.companion.core.dice.DamageRoller
import com.cosmere.companion.core.dice.Die
import com.cosmere.companion.core.dice.RollMode
import com.cosmere.companion.core.dice.SkillTestResult
import com.cosmere.companion.core.dice.SkillTestRoller

/**
 * Inline dice rolling shared by every "roll this" control on the character
 * sheet (skills, weapon attacks/damage): a tap on the die icon rolls
 * immediately and the result appears right under that row, rather than
 * navigating to a separate screen. A long-press on a skill-test die opens
 * mode (advantage/disadvantage) and Plot Die options first.
 *
 * State is hoisted to each call site (one [SkillRollUiState] per rollable
 * row, one nullable [DamageRollResult] per damage button) so the trigger icon
 * can sit inline in a row of controls while its result/options panel renders
 * as a full-width sibling underneath, in the caller's own layout.
 */
sealed interface SkillRollUiState {
    data object Collapsed : SkillRollUiState
    data object PickingOptions : SkillRollUiState
    data class ShowingResult(val result: SkillTestResult) : SkillRollUiState
}

private val skillTestRoller = SkillTestRoller()
private val damageRoller = DamageRoller()

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiceTriggerIcon(
    contentDescription: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Casino, contentDescription = contentDescription)
    }
}

/** The die icon for a skill test; place inline among a row's other controls. */
@Composable
fun SkillRollTrigger(
    label: String,
    modifierValue: Int,
    onStateChange: (SkillRollUiState) -> Unit,
) {
    DiceTriggerIcon(
        contentDescription = "Roll $label",
        onClick = { onStateChange(SkillRollUiState.ShowingResult(skillTestRoller.roll(modifier = modifierValue))) },
        onLongClick = { onStateChange(SkillRollUiState.PickingOptions) },
    )
}

/** The options/result panel for a skill test; place as a full-width sibling below the row holding [SkillRollTrigger]. */
@Composable
fun SkillRollExpansion(
    modifierValue: Int,
    state: SkillRollUiState,
    onStateChange: (SkillRollUiState) -> Unit,
) {
    when (state) {
        SkillRollUiState.Collapsed -> Unit
        SkillRollUiState.PickingOptions -> RollOptionsCard(
            onRoll = { mode, plotDie ->
                onStateChange(
                    SkillRollUiState.ShowingResult(
                        skillTestRoller.roll(modifier = modifierValue, mode = mode, rollPlotDie = plotDie),
                    ),
                )
            },
            onCancel = { onStateChange(SkillRollUiState.Collapsed) },
        )
        is SkillRollUiState.ShowingResult -> SkillTestResultCard(
            result = state.result,
            onDismiss = { onStateChange(SkillRollUiState.Collapsed) },
        )
    }
}

@Composable
private fun RollOptionsCard(onRoll: (RollMode, Boolean) -> Unit, onCancel: () -> Unit) {
    var mode by remember { mutableStateOf(RollMode.NORMAL) }
    var plotDie by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RollMode.entries.forEach { rollMode ->
                    FilterChip(
                        selected = mode == rollMode,
                        onClick = { mode = rollMode },
                        label = { Text(rollMode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = plotDie, onCheckedChange = { plotDie = it })
                Text("Raise the stakes (Plot Die)", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onRoll(mode, plotDie) }) { Text("Roll") }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun SkillTestResultCard(result: SkillTestResult, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Total: ${result.total}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "d20: ${result.d20Rolls.joinToString()} (kept ${result.keptD20})${formatModifierSuffix(result.modifier)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                result.plotDie?.let { face ->
                    val label = when {
                        face.isOpportunity -> "Plot die: Opportunity!"
                        face.isComplication -> "Plot die: Complication (+${face.complicationBonus})"
                        else -> "Plot die: Blank"
                    }
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
                if (result.hasNaturalOpportunity) {
                    Text("Natural ${result.keptD20}: Opportunity!", style = MaterialTheme.typography.bodySmall)
                }
                if (result.hasNaturalComplication) {
                    Text("Natural ${result.keptD20}: Complication", style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss roll result")
            }
        }
    }
}

/** The die icon for a damage roll; place inline among a row's other controls. */
@Composable
fun DamageRollTrigger(label: String, die: Die, count: Int = 1, flatBonus: Int = 0, onResult: (DamageRollResult) -> Unit) {
    DiceTriggerIcon(
        contentDescription = "Roll damage for $label",
        onClick = { onResult(damageRoller.roll(die, count, flatBonus)) },
    )
}

/** The result panel for a damage roll; place as a full-width sibling below the row holding [DamageRollTrigger]. */
@Composable
fun DamageRollExpansion(result: DamageRollResult?, onDismiss: () -> Unit) {
    result?.let {
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Damage: ${it.total}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    val bonusSuffix = if (it.flatBonus != 0) formatModifierSuffix(it.flatBonus) else ""
                    Text("${it.die}: ${it.rolls.joinToString()}$bonusSuffix", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Dismiss damage result")
                }
            }
        }
    }
}

private fun formatModifierSuffix(modifier: Int): String = when {
    modifier > 0 -> " +$modifier"
    modifier < 0 -> " $modifier"
    else -> ""
}

/** Parses a book damage string like "1d6 keen" or "2d10 impact" into a rollable die + count, or null if freeform (e.g. "Unique"). */
fun parseDamageDie(damage: String): Pair<Die, Int>? {
    val match = Regex("""(\d+)d(\d+)""").find(damage) ?: return null
    val count = match.groupValues[1].toIntOrNull() ?: return null
    val sides = match.groupValues[2].toIntOrNull() ?: return null
    val die = Die.entries.find { it.sides == sides } ?: return null
    return die to count
}
