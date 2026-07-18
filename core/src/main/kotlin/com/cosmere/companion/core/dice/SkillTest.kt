package com.cosmere.companion.core.dice

import kotlin.random.Random

/**
 * How multiple d20s are rolled for a test.
 *
 * Advantage/disadvantage dice add extra d20s; the best (or worst) result is
 * kept. Net advantage and disadvantage cancel each other out before rolling.
 */
enum class RollMode {
    NORMAL,
    ADVANTAGE,
    DISADVANTAGE;
}

/** The result of a single skill test roll. */
data class SkillTestResult(
    val d20Rolls: List<Int>,
    val keptD20: Int,
    val modifier: Int,
    val plotDie: PlotDieFace?,
    val opportunityRangeStart: Int = 20,
    val complicationRangeEnd: Int = 1,
) {
    /** Total = kept d20 + modifier + any complication bonus from the plot die. */
    val total: Int
        get() = keptD20 + modifier + (plotDie?.complicationBonus ?: 0)

    /** Natural results within the opportunity range grant an Opportunity. */
    val hasNaturalOpportunity: Boolean
        get() = keptD20 >= opportunityRangeStart

    /** Natural results within the complication range add a Complication (no bonus). */
    val hasNaturalComplication: Boolean
        get() = keptD20 <= complicationRangeEnd

    val opportunityCount: Int
        get() = (if (hasNaturalOpportunity) 1 else 0) + (if (plotDie?.isOpportunity == true) 1 else 0)

    val complicationCount: Int
        get() = (if (hasNaturalComplication) 1 else 0) + (if (plotDie?.isComplication == true) 1 else 0)

    fun beats(difficulty: Int): Boolean = total >= difficulty
}

/** Rolls skill tests per the Cosmere RPG core mechanic. */
class SkillTestRoller(private val random: Random = Random.Default) {

    fun roll(
        modifier: Int = 0,
        mode: RollMode = RollMode.NORMAL,
        extraDice: Int = 1,
        rollPlotDie: Boolean = false,
        opportunityRangeStart: Int = 20,
        complicationRangeEnd: Int = 1,
    ): SkillTestResult {
        val count = if (mode == RollMode.NORMAL) 1 else 1 + extraDice.coerceAtLeast(1)
        val rolls = List(count) { Die.D20.roll(random) }
        val kept = when (mode) {
            RollMode.NORMAL -> rolls.first()
            RollMode.ADVANTAGE -> rolls.max()
            RollMode.DISADVANTAGE -> rolls.min()
        }
        return SkillTestResult(
            d20Rolls = rolls,
            keptD20 = kept,
            modifier = modifier,
            plotDie = if (rollPlotDie) PlotDieFace.roll(random) else null,
            opportunityRangeStart = opportunityRangeStart,
            complicationRangeEnd = complicationRangeEnd,
        )
    }
}

/** The result of rolling one or more damage dice. */
data class DamageRollResult(
    val die: Die,
    val rolls: List<Int>,
    val flatBonus: Int,
) {
    val total: Int get() = rolls.sum() + flatBonus
}

/** Rolls damage dice (e.g. 3d6 + 2). */
class DamageRoller(private val random: Random = Random.Default) {
    fun roll(die: Die, count: Int = 1, flatBonus: Int = 0): DamageRollResult =
        DamageRollResult(
            die = die,
            rolls = List(count.coerceAtLeast(1)) { die.roll(random) },
            flatBonus = flatBonus,
        )
}
