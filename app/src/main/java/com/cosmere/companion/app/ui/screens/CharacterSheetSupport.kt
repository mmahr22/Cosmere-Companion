package com.cosmere.companion.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration

/**
 * Small display helpers shared across the roster, sheet, level-up dialog, and talents section —
 * split out on their own since none of them belongs to any single feature area.
 */

/** A tappable name that jumps to the matching entry in the Reference tab. */
@Composable
internal fun SheetLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** Formats a points-remaining/over-budget suffix, e.g. " (2 remaining)" or " (1 over)". */
internal fun pointsSuffix(remaining: Int): String = when {
    remaining > 0 -> " ($remaining remaining)"
    remaining < 0 -> " (${-remaining} over)"
    else -> ""
}

@Composable
internal fun DeleteCharacterDialog(characterName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
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
