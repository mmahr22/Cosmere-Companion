package com.cosmere.companion.core.dice

import kotlin.random.Random

/** A standard polyhedral die used by the Cosmere RPG. */
enum class Die(val sides: Int) {
    D4(4),
    D6(6),
    D8(8),
    D10(10),
    D12(12),
    D20(20);

    fun roll(random: Random = Random.Default): Int = random.nextInt(1, sides + 1)

    override fun toString(): String = "d$sides"
}

/**
 * One face of the plot die. Two faces are blank, two show an Opportunity,
 * and two show a Complication. A Complication also grants a bonus (+2 or +4)
 * to the test it complicates.
 */
enum class PlotDieFace(val complicationBonus: Int = 0) {
    BLANK_1,
    BLANK_2,
    OPPORTUNITY_1,
    OPPORTUNITY_2,
    COMPLICATION_PLUS_2(complicationBonus = 2),
    COMPLICATION_PLUS_4(complicationBonus = 4);

    val isOpportunity: Boolean
        get() = this == OPPORTUNITY_1 || this == OPPORTUNITY_2

    val isComplication: Boolean
        get() = complicationBonus > 0

    companion object {
        /** Faces in the order matching a standard d6 (1..6) for substitution rolls. */
        val byD6Value: List<PlotDieFace> = listOf(
            COMPLICATION_PLUS_2, // 1
            COMPLICATION_PLUS_4, // 2
            BLANK_1,             // 3
            BLANK_2,             // 4
            OPPORTUNITY_1,       // 5
            OPPORTUNITY_2,       // 6
        )

        fun roll(random: Random = Random.Default): PlotDieFace =
            byD6Value[random.nextInt(byD6Value.size)]
    }
}
