package com.cosmere.companion.app.data

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Copies a picked photo into app-private storage (content URIs from the
 * system photo picker aren't guaranteed to stay readable across app
 * restarts) and returns its absolute path. Deletes [previousPath] if this
 * replaces an existing avatar — the new file gets a unique name so a
 * cached image (e.g. Coil) never has a stale-path collision.
 */
fun saveAvatar(context: Context, characterId: Int, source: Uri, previousPath: String?): String {
    deleteAvatar(previousPath)
    val dir = File(context.filesDir, "avatars").apply { mkdirs() }
    val file = File(dir, "avatar_${characterId}_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(source)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    return file.absolutePath
}

fun deleteAvatar(path: String?) {
    path?.let { File(it).delete() }
}
