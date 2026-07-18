package com.cosmere.companion.core.model

import kotlinx.serialization.Serializable

/** How a talent is activated. */
enum class Activation {
    ACTION1,
    ACTION2,
    ACTION3,
    FREE,
    REACTION,
    SPECIAL,
    PASSIVE;
}

/** A heroic or Radiant path. */
@Serializable
data class GamePath(
    val id: String,
    val name: String,
    val type: String,
    val startingSkillId: String? = null,
    val keyTalentId: String,
    val specialties: List<String> = emptyList(),
    val summary: String,
    val attributeTips: List<String> = emptyList(),
    val skillTips: List<String> = emptyList(),
    /** Radiant paths only: the bonded spren type (e.g. "ashspren"). */
    val sprenType: String? = null,
    /** Radiant paths only: ids of the order's two surges. */
    val surgeIds: List<String> = emptyList(),
    val page: Int? = null,
)

/** A single talent in a path's talent tree. */
@Serializable
data class Talent(
    val id: String,
    val name: String,
    val pathId: String,
    val specialty: String? = null,
    val isKey: Boolean = false,
    val activation: String,
    val focusCost: Int? = null,
    val prerequisiteSkills: Map<String, Int> = emptyMap(),
    val prerequisiteTalents: List<String> = emptyList(),
    /** "all" (default) requires every listed talent; "any" requires at least one. */
    val prerequisiteTalentsMode: String = "all",
    /** Minimum character level, if any (e.g. Ideal talents). */
    val prerequisiteLevel: Int? = null,
    /** Radiant only: the Nth Ideal that must already be SPOKEN (goal completed). */
    val prerequisiteIdealSpoken: Int? = null,
    val prerequisiteOther: String? = null,
    val summary: String,
    val page: Int? = null,
) {
    val activationType: Activation
        get() = when (activation.lowercase()) {
            "action1" -> Activation.ACTION1
            "action2" -> Activation.ACTION2
            "action3" -> Activation.ACTION3
            "free" -> Activation.FREE
            "reaction" -> Activation.REACTION
            "special" -> Activation.SPECIAL
            else -> Activation.PASSIVE
        }
}

@Serializable
internal data class PathsFile(val paths: List<GamePath>)

@Serializable
internal data class TalentsFile(val talents: List<Talent>)
