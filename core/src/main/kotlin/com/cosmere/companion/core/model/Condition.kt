package com.cosmere.companion.core.model

import kotlinx.serialization.Serializable

/**
 * A temporary condition that can apply to a character.
 *
 * [hasBracketValue] marks conditions that carry a value in brackets when
 * applied (e.g. a damage amount, attribute bonus, or test penalty).
 * [cumulative] marks the few conditions where multiple instances stack
 * instead of being ignored.
 */
@Serializable
data class Condition(
    val id: String,
    val name: String,
    val summary: String,
    val hasBracketValue: Boolean = false,
    val cumulative: Boolean = false,
    val page: Int? = null,
)

@Serializable
internal data class ConditionsFile(val conditions: List<Condition>)
