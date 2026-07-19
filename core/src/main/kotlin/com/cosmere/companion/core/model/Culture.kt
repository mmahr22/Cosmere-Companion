package com.cosmere.companion.core.model

import kotlinx.serialization.Serializable

/**
 * A Rosharan cultural expertise a character can choose in step 1 of character
 * creation (two picks total, from any mix of cultures).
 *
 * [singerOnly] marks the Listener expertise, which the book restricts to
 * singer characters (or humans with GM permission).
 */
@Serializable
data class Culture(
    val id: String,
    val name: String,
    val summary: String,
    val names: List<String> = emptyList(),
    val expertiseSummary: String,
    val singerOnly: Boolean = false,
    val page: Int? = null,
)

@Serializable
internal data class CulturesFile(val cultures: List<Culture>)
