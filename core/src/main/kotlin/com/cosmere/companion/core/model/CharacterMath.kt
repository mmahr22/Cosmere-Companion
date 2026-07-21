package com.cosmere.companion.core.model

/**
 * Derived-statistic calculations for player characters.
 *
 * Formulas verified against chapter 1 ("Final Calculations"):
 * - Physical defense  = 10 + Strength + Speed
 * - Cognitive defense = 10 + Intellect + Willpower
 * - Spiritual defense = 10 + Awareness + Presence
 * - Max health (level 1) = 10 + Strength
 * - Max focus            = 2 + Willpower
 * - Investiture starts at 0 until a path grants it
 */
object CharacterMath {

    const val CREATION_ATTRIBUTE_POINTS = 12
    const val CREATION_ATTRIBUTE_MAX = 3
    const val ATTRIBUTE_LEVEL_CAP = 5
    const val CREATION_FREE_SKILL_RANKS = 4

    fun defense(defense: Defense, attributes: Map<Attribute, Int>): Int {
        val pair = Attribute.entries.filter { it.defense == defense }
        return 10 + pair.sumOf { attributes[it] ?: 0 }
    }

    fun maxFocus(willpower: Int): Int = 2 + willpower

    /**
     * Lifting/Carrying Capacity table (chapter 3, "Strength" section): the
     * max weight (lb.) a character can lift over their head in one attempt,
     * or comfortably carry while walking, at a given Strength score.
     */
    fun liftingCapacityLb(strength: Int): Int = when {
        strength <= 0 -> 100
        strength <= 2 -> 200
        strength <= 4 -> 500
        strength <= 6 -> 1000
        strength <= 8 -> 5000
        else -> 10000
    }

    fun carryingCapacityLb(strength: Int): Int = when {
        strength <= 0 -> 50
        strength <= 2 -> 100
        strength <= 4 -> 250
        strength <= 6 -> 500
        strength <= 8 -> 2500
        else -> 5000
    }

    /**
     * Maximum health from the Character Advancement table: 10 + STR at
     * level 1, then +5/level through 5, +4 through 10, +3 through 15,
     * +2 through 20, and +1 per level beyond. Levels 6, 11, and 16 add
     * the Strength attribute again on top of the flat gain.
     */
    fun maxHealth(level: Int, strength: Int): Int {
        require(level >= 1) { "level must be >= 1" }
        var health = 10 + strength
        for (l in 2..level) {
            health += when {
                l <= 5 -> 5
                l <= 10 -> 4
                l <= 15 -> 3
                l <= 20 -> 2
                else -> 1
            }
            if (l == 6 || l == 11 || l == 16) health += strength
        }
        return health
    }

    /** Tier of play for a character level (1..5). */
    fun tier(level: Int): Int = when {
        level <= 5 -> 1
        level <= 10 -> 2
        level <= 15 -> 3
        level <= 20 -> 4
        else -> 5
    }

    /** Maximum ranks allowed in a single skill, by tier. */
    fun maxSkillRank(level: Int): Int = minOf(tier(level) + 1, 5)

    /** Levels that grant a +1 attribute increase (max 5 per attribute). */
    val ATTRIBUTE_INCREASE_LEVELS = setOf(3, 6, 9, 12, 15, 18)

    /**
     * Talent-point budget for [level], from the "Gain a Talent" advancement
     * step: one talent per level from 2 through 20 (level 1's only talent is
     * the free heroic/Radiant/ancestry key talent, granted separately and
     * not counted against this budget), plus one bonus pick per level in
     * [ancestryBonusTalentLevels] reached so far (see [Ancestry.bonusTalentLevels],
     * which varies by ancestry — e.g. Singers don't get theirs until level 6,
     * since their level-1 pick is the forced Change Form key talent instead).
     * Levels 21+ grant a shared "skill rank OR talent" choice (see
     * [totalSkillRanks]'s doc), so — matching that function's convention —
     * only the guaranteed floor is counted here.
     */
    fun totalTalentPoints(level: Int, ancestryBonusTalentLevels: List<Int> = emptyList()): Int =
        minOf(level - 1, 19) + ancestryBonusTalentLevels.count { it <= level }

    /** Total skill ranks a character should have at [level] (4 at creation +1 path start, +2/level). */
    fun totalSkillRanks(level: Int): Int =
        if (level <= 20) {
            CREATION_FREE_SKILL_RANKS + 1 + 2 * (level - 1)
        } else {
            // Level 21+: choice of +1 skill rank OR +1 talent per level, so
            // only a floor can be computed; callers track the actual choice.
            CREATION_FREE_SKILL_RANKS + 1 + 2 * 19
        }

    /** Total attribute points a character should have allocated at [level] (12 at creation, +1 per [ATTRIBUTE_INCREASE_LEVELS] milestone reached). */
    fun totalAttributePoints(level: Int): Int =
        CREATION_ATTRIBUTE_POINTS + ATTRIBUTE_INCREASE_LEVELS.count { it <= level }
}

/**
 * UNVERIFIED derived-stat tables.
 *
 * The printed lookup tables live in chapter 3 pages we don't have yet.
 * Anchors confirmed from worked examples elsewhere in the book:
 * movement 30 ft at Speed 3 and 40 ft at Speed 5; recovery die d6 at
 * Willpower 2; senses range 20 ft at Awareness 3. The linear
 * interpolations below fit those anchors but MUST be re-checked against
 * the chapter 3 tables before being trusted.
 */
object UnverifiedDerivedStats {
    fun movementFeet(speed: Int): Int = 15 + 5 * speed.coerceIn(0, 9)

    fun recoveryDie(willpower: Int): Die = when {
        willpower <= 0 -> Die.D4
        willpower <= 2 -> Die.D6
        willpower <= 4 -> Die.D8
        willpower <= 6 -> Die.D10
        else -> Die.D12
    }

    fun sensesRangeFeet(awareness: Int): Int = 5 + 5 * awareness.coerceIn(0, 9)
}

typealias Die = com.cosmere.companion.core.dice.Die
