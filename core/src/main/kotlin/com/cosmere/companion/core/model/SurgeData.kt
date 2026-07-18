package com.cosmere.companion.core.model

import kotlinx.serialization.Serializable

/** Shared scaling rules for all surges, keyed by ranks in the surge skill. */
@Serializable
data class SurgeScalingRow(
    val ranks: Int,
    val dieSize: Int,
    val effectSizeName: String,
    val effectSizeFeet: Double,
)

/** A talent belonging to a surge's upgrade tree (shared by both orders that wield it). */
@Serializable
data class SurgeTalent(
    val id: String,
    val name: String,
    val activation: String,
    val focusCost: Int? = null,
    val investitureCost: Int? = null,
    val prerequisiteSkills: Map<String, Int> = emptyMap(),
    val prerequisiteTalents: List<String> = emptyList(),
    val prerequisiteTalentsMode: String = "all",
    val prerequisiteIdealSpoken: Int? = null,
    val prerequisiteOther: String? = null,
    val summary: String,
    val page: Int? = null,
)

/** One of the ten surges: its base ability plus its talent tree. */
@Serializable
data class SurgeEntry(
    val id: String,
    val name: String,
    val attributeId: String,
    val radiantOrders: List<String>,
    val summary: String,
    val talents: List<SurgeTalent> = emptyList(),
    val page: Int? = null,
)

/** One row of the Transformation Difficulty Classes table (chapter 6). */
@Serializable
data class TransformationDcRow(
    val fromCategory: String,
    val toSolids: Int,
    val toOrganics: Int,
    val toLiquids: Int,
    val toVapors: Int,
    val toClearAir: Int,
    val toFlame: Int? = null,
)

@Serializable
internal data class SurgesFile(
    val scaling: List<SurgeScalingRow>,
    val surges: List<SurgeEntry>,
    val transformationDcTable: List<TransformationDcRow> = emptyList(),
)
