package com.cosmere.companion.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.cosmere.companion.core.model.Attribute
import com.cosmere.companion.core.model.PlayerCharacter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MapConverters {
    @TypeConverter
    fun fromIntMap(map: Map<String, Int>): String = Json.encodeToString(map)

    @TypeConverter
    fun toIntMap(value: String): Map<String, Int> = Json.decodeFromString(value)

    @TypeConverter
    fun fromStringList(list: List<String>): String = Json.encodeToString(list)

    @TypeConverter
    fun toStringList(value: String): List<String> = Json.decodeFromString(value)
}

/**
 * Only one row is ever stored (fixed [id]): this app tracks a single active
 * character sheet, not a roster, so persistence just needs to survive
 * process death across app restarts.
 */
@Entity(tableName = "player_character")
@TypeConverters(MapConverters::class)
data class PlayerCharacterEntity(
    @PrimaryKey val id: Int = CURRENT_CHARACTER_ID,
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
) {
    companion object {
        const val CURRENT_CHARACTER_ID = 0
    }
}

fun PlayerCharacter.toEntity(): PlayerCharacterEntity = PlayerCharacterEntity(
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
)

fun PlayerCharacterEntity.toDomain(): PlayerCharacter = PlayerCharacter(
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
)
