package com.cosmere.companion.core.data

import com.cosmere.companion.core.model.Condition
import com.cosmere.companion.core.model.ConditionsFile
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

    fun conditionById(id: String): Condition? = conditions.firstOrNull { it.id == id }

    private fun readResource(path: String): String =
        requireNotNull(RulesRepository::class.java.getResource(path)) {
            "Missing rules resource: $path"
        }.readText()
}
