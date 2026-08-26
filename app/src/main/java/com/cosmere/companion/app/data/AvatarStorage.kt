package com.cosmere.companion.app.data

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Copies a picked photo into app-private storage (content URIs from the
 * system photo picker aren't guaranteed to stay readable across app
 * restarts) and returns its absolute path. Deletes [previousPath] if this
 * replaces an existing avatar — the new file gets a unique name so a
 * cached image (e.g. Coil) never has a stale-path collision.
 *
 * Suspends onto [Dispatchers.IO] since copying a full-resolution photo is
 * real disk/stream I/O, not safe to run on the caller's (typically main)
 * thread.
 */
suspend fun saveAvatar(context: Context, characterId: Int, source: Uri, previousPath: String?): String =
    withContext(Dispatchers.IO) {
        deleteAvatar(previousPath)
        val dir = File(context.filesDir, "avatars").apply { mkdirs() }
        val file = File(dir, "avatar_${characterId}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(source)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file.absolutePath
    }

suspend fun deleteAvatar(path: String?) {
    if (path == null) return
    withContext(Dispatchers.IO) { File(path).delete() }
}
