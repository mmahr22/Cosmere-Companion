package com.cosmere.companion.app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.cosmere.companion.core.model.Attribute
import com.cosmere.companion.core.model.PlayerCharacter
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The on-disk shape of a shared character file — deliberately independent of
 * [com.cosmere.companion.app.data.PlayerCharacterEntity] (whose Room-specific
 * `id` shouldn't round-trip through a share/import, since the imported copy
 * always becomes a new row) and of the avatar image (a local file path that
 * can't resolve on another device or after a reinstall).
 */
@Serializable
private data class CharacterExport(
    val name: String,
    val ancestryId: String?,
    val cultureIds: List<String>,
    val level: Int,
    val attributes: Map<String, Int>,
    val heroicPathId: String,
    val specialty: String?,
    val radiantPathId: String?,
    val skillRanks: Map<String, Int>,
    val currentHealth: Int,
    val currentFocus: Int,
    val currentInvestiture: Int,
    val maxInvestiture: Int,
    val unlockedFormIds: List<String>,
    val currentFormId: String?,
    val inventory: Map<String, Int>,
    val equippedWeaponIds: List<String>,
    val equippedArmorId: String?,
    val bonusAttributePoints: Int,
    val bonusSkillPoints: Int,
    val notes: String,
    val activeConditions: Map<String, Int>,
    val purchasedTalentIds: List<String>,
    val bonusTalentPoints: Int,
)

/** A lightweight wrapper so an arbitrary/unrelated JSON file is rejected on import instead of silently misparsed. */
@Serializable
private data class CharacterExportFile(
    val format: String,
    val character: CharacterExport,
)

private const val EXPORT_FORMAT = "cosmere-companion-character"

private fun PlayerCharacter.toExport(): CharacterExport = CharacterExport(
    name = name,
    ancestryId = ancestryId,
    cultureIds = cultureIds,
    level = level,
    attributes = attributes.mapKeys { it.key.name },
    heroicPathId = heroicPathId,
    specialty = specialty,
    radiantPathId = radiantPathId,
    skillRanks = skillRanks,
    currentHealth = currentHealth,
    currentFocus = currentFocus,
    currentInvestiture = currentInvestiture,
    maxInvestiture = maxInvestiture,
    unlockedFormIds = unlockedFormIds,
    currentFormId = currentFormId,
    inventory = inventory,
    equippedWeaponIds = equippedWeaponIds,
    equippedArmorId = equippedArmorId,
    bonusAttributePoints = bonusAttributePoints,
    bonusSkillPoints = bonusSkillPoints,
    notes = notes,
    activeConditions = activeConditions,
    purchasedTalentIds = purchasedTalentIds,
    bonusTalentPoints = bonusTalentPoints,
)

private fun CharacterExport.toDomain(): PlayerCharacter = PlayerCharacter(
    id = 0,
    name = name,
    ancestryId = ancestryId,
    cultureIds = cultureIds,
    level = level,
    attributes = attributes.mapNotNull { (key, value) ->
        Attribute.entries.find { it.name == key }?.let { attribute -> attribute to value }
    }.toMap(),
    heroicPathId = heroicPathId,
    specialty = specialty,
    radiantPathId = radiantPathId,
    skillRanks = skillRanks,
    currentHealth = currentHealth,
    currentFocus = currentFocus,
    currentInvestiture = currentInvestiture,
    maxInvestiture = maxInvestiture,
    unlockedFormIds = unlockedFormIds,
    currentFormId = currentFormId,
    inventory = inventory,
    equippedWeaponIds = equippedWeaponIds,
    equippedArmorId = equippedArmorId,
    bonusAttributePoints = bonusAttributePoints,
    bonusSkillPoints = bonusSkillPoints,
    notes = notes,
    activeConditions = activeConditions,
    purchasedTalentIds = purchasedTalentIds,
    bonusTalentPoints = bonusTalentPoints,
)

/**
 * Writes [character] as JSON into the app's cache dir and returns a
 * `content://` [Uri] (via [FileProvider]) suitable for a share-sheet intent.
 */
fun writeCharacterExport(context: Context, character: PlayerCharacter): Uri {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val safeName = character.name.ifBlank { "character" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val file = File(dir, "$safeName.json")
    val export = CharacterExportFile(format = EXPORT_FORMAT, character = character.toExport())
    file.writeText(Json.encodeToString(export))
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/**
 * Reads and decodes a character export from [uri]. Throws if [uri] isn't
 * readable or isn't a recognized export file — callers should catch broadly
 * and show a simple "couldn't read that file" message.
 */
fun readCharacterExport(context: Context, uri: Uri): PlayerCharacter {
    val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        ?: error("Unable to open $uri")
    val export = Json.decodeFromString<CharacterExportFile>(text)
    require(export.format == EXPORT_FORMAT) { "Not a Cosmere Companion character file" }
    return export.character.toDomain()
}
