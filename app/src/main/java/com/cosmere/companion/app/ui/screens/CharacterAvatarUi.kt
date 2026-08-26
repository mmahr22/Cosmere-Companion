package com.cosmere.companion.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/** A character's picked photo, or a colored initials badge if none has been set yet. */
@Composable
internal fun CharacterAvatar(
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

private val AVATAR_COLORS = listOf(
    Color(0xFF6750A4), Color(0xFF386A20), Color(0xFFB3261E), Color(0xFF0061A4), Color(0xFF8B5000),
)

private fun avatarColorFor(name: String): Color = AVATAR_COLORS[(name.hashCode() and 0x7FFFFFFF) % AVATAR_COLORS.size]

private fun initialsFor(name: String): String =
    name.trim().split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
