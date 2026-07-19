package com.cosmere.companion.core.data

import com.cosmere.companion.core.model.Ancestry
import com.cosmere.companion.core.model.AncestriesFile
import com.cosmere.companion.core.model.Condition
import com.cosmere.companion.core.model.ConditionsFile
import com.cosmere.companion.core.model.Culture
import com.cosmere.companion.core.model.CulturesFile
import com.cosmere.companion.core.model.GamePath
import com.cosmere.companion.core.model.PathsFile
import com.cosmere.companion.core.model.SurgeEntry
import com.cosmere.companion.core.model.SurgeScalingRow
import com.cosmere.companion.core.model.SurgesFile
import com.cosmere.companion.core.model.Talent
import com.cosmere.companion.core.model.TalentsFile
import kotlinx.serialization.json.Json

/**
 * Loads static rules data bundled as JSON resources in this module.
 *
 * Data files live in `core/src/main/resources/rules/` and are packaged on
 * the classpath, so both the Android app and JVM tests can read them the
 * same way.
 */
object RulesRepository {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    val conditions: List<Condition> by lazy {
        json.decodeFromString<ConditionsFile>(readResource("/rules/conditions.json")).conditions
    }

    val paths: List<GamePath> by lazy {
        json.decodeFromString<PathsFile>(readResource("/rules/paths.json")).paths
    }

    val talents: List<Talent> by lazy {
        json.decodeFromString<TalentsFile>(readResource("/rules/talents.json")).talents
    }

    val ancestries: List<Ancestry> by lazy {
        json.decodeFromString<AncestriesFile>(readResource("/rules/ancestries.json")).ancestries
    }

    val cultures: List<Culture> by lazy {
        json.decodeFromString<CulturesFile>(readResource("/rules/cultures.json")).cultures
    }

    private val surgesFile: SurgesFile by lazy {
        json.decodeFromString<SurgesFile>(readResource("/rules/surges.json"))
    }

    val surges: List<SurgeEntry> get() = surgesFile.surges

    val surgeScaling: List<SurgeScalingRow> get() = surgesFile.scaling

    val transformationDcTable: List<com.cosmere.companion.core.model.TransformationDcRow>
        get() = surgesFile.transformationDcTable

    fun conditionById(id: String): Condition? = conditions.firstOrNull { it.id == id }

    fun pathById(id: String): GamePath? = paths.firstOrNull { it.id == id }

    fun talentsForPath(pathId: String): List<Talent> = talents.filter { it.pathId == pathId }

    fun surgeById(id: String): SurgeEntry? = surges.firstOrNull { it.id == id }

    fun ancestryById(id: String): Ancestry? = ancestries.firstOrNull { it.id == id }

    fun cultureById(id: String): Culture? = cultures.firstOrNull { it.id == id }

    private fun readResource(path: String): String =
        requireNotNull(RulesRepository::class.java.getResource(path)) {
            "Missing rules resource: $path"
        }.readText()
}
