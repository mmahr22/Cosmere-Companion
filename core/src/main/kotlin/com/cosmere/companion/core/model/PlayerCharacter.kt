package com.cosmere.companion.core.model

/**
 * A player character sheet: identity, attributes, path choices, skill ranks,
 * and the resource pools tracked during play.
 *
 * Skill ranks are keyed by [Skill.name] for the eighteen standard skills and
 * by a surge's id (see [SurgeEntry.id]) once a Radiant order's surges are
 * unlocked.
 *
 * Ancestry and culture are freeform flavor text: this app doesn't bundle
 * ancestry/culture mechanical data (attribute bonuses, abilities, etc.) yet,
 * so no fixed list is offered here.
 *
 * There's no printed formula for maximum Investiture in the data bundled so
 * far (unlike health/focus, which [CharacterMath] derives directly from the
 * book's worked examples), so [maxInvestiture] is tracked as a plain value
 * the player sets rather than a computed one.
 */
data class PlayerCharacter(
    val name: String,
    val ancestry: String = "",
    val culture: String = "",
    val level: Int = 1,
    val attributes: Map<Attribute, Int> = Attribute.entries.associateWith { 0 },
    val heroicPathId: String,
    val specialty: String? = null,
    val radiantPathId: String? = null,
    val skillRanks: Map<String, Int> = emptyMap(),
    val currentHealth: Int = CharacterMath.maxHealth(level, attributes[Attribute.STRENGTH] ?: 0),
    val currentFocus: Int = CharacterMath.maxFocus(attributes[Attribute.WILLPOWER] ?: 0),
    val currentInvestiture: Int = 0,
    val maxInvestiture: Int = 0,
) {
    fun attribute(attribute: Attribute): Int = attributes[attribute] ?: 0

    fun defense(defense: Defense): Int = CharacterMath.defense(defense, attributes)

    val maxHealth: Int get() = CharacterMath.maxHealth(level, attribute(Attribute.STRENGTH))

    val maxFocus: Int get() = CharacterMath.maxFocus(attribute(Attribute.WILLPOWER))

    fun skillRank(skillId: String): Int = skillRanks[skillId] ?: 0
}
