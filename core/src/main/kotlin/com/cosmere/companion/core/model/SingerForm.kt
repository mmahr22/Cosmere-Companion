package com.cosmere.companion.core.model

import kotlinx.serialization.Serializable

/**
 * A form a Singer character can hold, per the Change Form key talent: only
 * one form is active at a time, and [grantedByTalentId] null marks the two
 * starting forms (dullform, mateform) granted by that key talent itself
 * rather than by a later Singer-tree talent.
 *
 * [attributeBonuses] (keyed by [Attribute.name]) are the only per-form
 * effects this app computes into the sheet's derived stats; other effects
 * (advantage on tests, special actions, deflect changes) are descriptive
 * only in [summary], matching how talent effects are handled elsewhere.
 */
@Serializable
data class SingerForm(
    val id: String,
    val name: String,
    val sprenBond: String,
    val summary: String,
    val attributeBonuses: Map<String, Int> = emptyMap(),
    val grantedByTalentId: String? = null,
    val voidform: Boolean = false,
    val page: Int? = null,
)

@Serializable
internal data class SingerFormsFile(val forms: List<SingerForm>)
