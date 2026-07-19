package com.cosmere.companion.core.model

import kotlinx.serialization.Serializable

/**
 * One of the two sapient species of Roshar (human or singer).
 *
 * [bonusTalentLevels] are the character levels at which this ancestry grants
 * an extra talent pick (always level 1, plus every tier boundary after).
 * [bonusTalentSource] describes which talent trees that pick may draw from.
 *
 * Singers additionally force [keyTalentId] ("Change Form") as an automatic
 * extra talent at level 1, on top of the normal bonus-talent pick.
 */
@Serializable
data class Ancestry(
    val id: String,
    val name: String,
    val size: String,
    val summary: String,
    val bonusTalentLevels: List<Int>,
    val bonusTalentSource: String,
    val keyTalentId: String? = null,
    val page: Int? = null,
)

@Serializable
internal data class AncestriesFile(val ancestries: List<Ancestry>)
