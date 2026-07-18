package com.cosmere.companion.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cosmere.companion.core.dice.DamageRoller
import com.cosmere.companion.core.dice.Die
import com.cosmere.companion.core.dice.RollMode
import com.cosmere.companion.core.dice.SkillTestResult
import com.cosmere.companion.core.dice.SkillTestRoller
import kotlin.math.roundToInt

@Composable
fun DiceScreen() {
    val skillRoller = remember { SkillTestRoller() }
    val damageRoller = remember { DamageRoller() }

    var modifier by remember { mutableStateOf(0f) }
    var mode by remember { mutableStateOf(RollMode.NORMAL) }
    var usePlotDie by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SkillTestResult?>(null) }
    var damageText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Skill Test", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RollMode.entries.forEach { rollMode ->
                FilterChip(
                    selected = mode == rollMode,
                    onClick = { mode = rollMode },
                    label = {
                        Text(
                            rollMode.name.lowercase().replaceFirstChar { it.uppercase() },
                        )
                    },
                )
            }
        }

        Text(formatModifier(modifier.roundToInt()))
        Slider(
            value = modifier,
            onValueChange = { modifier = it },
            valueRange = -5f..15f,
            steps = 19,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Switch(checked = usePlotDie, onCheckedChange = { usePlotDie = it })
            Text("Raise the stakes (roll the plot die)")
        }

        Button(
            onClick = {
                damageText = null
                result = skillRoller.roll(
                    modifier = modifier.roundToInt(),
                    mode = mode,
                    rollPlotDie = usePlotDie,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Roll d20")
        }

        result?.let { RollResultCard(it) }

        Text("Damage", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Die.entries.filter { it != Die.D20 }.forEach { die ->
                OutlinedButton(onClick = {
                    result = null
                    val roll = damageRoller.roll(die)
                    damageText = "$die → ${roll.total}"
                }) {
                    Text(die.toString())
                }
            }
        }
        damageText?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

private fun formatModifier(value: Int): String =
    "Skill modifier: " + if (value >= 0) "+$value" else "$value"

@Composable
private fun RollResultCard(result: SkillTestResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Total: ${result.total}",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text("d20: ${result.d20Rolls.joinToString()} (kept ${result.keptD20})")
            result.plotDie?.let { face ->
                val label = when {
                    face.isOpportunity -> "Opportunity!"
                    face.isComplication -> "Complication (+${face.complicationBonus} to total)"
                    else -> "Blank"
                }
                Text("Plot die: $label")
            }
            if (result.hasNaturalOpportunity) {
                Text("Natural ${result.keptD20}: Opportunity!")
            }
            if (result.hasNaturalComplication) {
                Text("Natural ${result.keptD20}: Complication")
            }
        }
    }
}
