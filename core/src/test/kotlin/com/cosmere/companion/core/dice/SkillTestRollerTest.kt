package com.cosmere.companion.core.dice

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SkillTestRollerTest {

    @Test
    fun `normal roll uses a single d20`() {
        val result = SkillTestRoller(Random(1)).roll(modifier = 3)
        assertEquals(1, result.d20Rolls.size)
        assertEquals(result.d20Rolls.first(), result.keptD20)
        assertEquals(result.keptD20 + 3, result.total)
        assertNull(result.plotDie)
    }

    @Test
    fun `advantage keeps the highest d20`() {
        val result = SkillTestRoller(Random(42)).roll(mode = RollMode.ADVANTAGE)
        assertEquals(2, result.d20Rolls.size)
        assertEquals(result.d20Rolls.max(), result.keptD20)
    }

    @Test
    fun `disadvantage keeps the lowest d20`() {
        val result = SkillTestRoller(Random(42)).roll(mode = RollMode.DISADVANTAGE)
        assertEquals(2, result.d20Rolls.size)
        assertEquals(result.d20Rolls.min(), result.keptD20)
    }

    @Test
    fun `complication faces add their bonus to the total`() {
        val result = SkillTestResult(
            d20Rolls = listOf(10),
            keptD20 = 10,
            modifier = 2,
            plotDie = PlotDieFace.COMPLICATION_PLUS_4,
        )
        assertEquals(16, result.total)
        assertEquals(1, result.complicationCount)
        assertEquals(0, result.opportunityCount)
    }

    @Test
    fun `natural 20 grants an opportunity by default`() {
        val result = SkillTestResult(d20Rolls = listOf(20), keptD20 = 20, modifier = 0, plotDie = null)
        assertTrue(result.hasNaturalOpportunity)
        assertFalse(result.hasNaturalComplication)
        assertEquals(1, result.opportunityCount)
    }

    @Test
    fun `natural 1 grants a complication with no bonus`() {
        val result = SkillTestResult(d20Rolls = listOf(1), keptD20 = 1, modifier = 5, plotDie = null)
        assertTrue(result.hasNaturalComplication)
        assertEquals(6, result.total)
    }

    @Test
    fun `expanded ranges shift natural opportunity and complication triggers`() {
        val result = SkillTestResult(
            d20Rolls = listOf(18),
            keptD20 = 18,
            modifier = 0,
            plotDie = null,
            opportunityRangeStart = 18,
            complicationRangeEnd = 3,
        )
        assertTrue(result.hasNaturalOpportunity)

        val low = result.copy(d20Rolls = listOf(3), keptD20 = 3)
        assertTrue(low.hasNaturalComplication)
    }

    @Test
    fun `plot die opportunity and natural 20 can stack`() {
        val result = SkillTestResult(
            d20Rolls = listOf(20),
            keptD20 = 20,
            modifier = 0,
            plotDie = PlotDieFace.OPPORTUNITY_1,
        )
        assertEquals(2, result.opportunityCount)
    }

    @Test
    fun `plot die d6 mapping matches the printed conversion diagram`() {
        assertEquals(PlotDieFace.COMPLICATION_PLUS_2, PlotDieFace.byD6Value[0])
        assertEquals(PlotDieFace.COMPLICATION_PLUS_4, PlotDieFace.byD6Value[1])
        assertEquals(PlotDieFace.BLANK_1, PlotDieFace.byD6Value[2])
        assertEquals(PlotDieFace.BLANK_2, PlotDieFace.byD6Value[3])
        assertEquals(PlotDieFace.OPPORTUNITY_1, PlotDieFace.byD6Value[4])
        assertEquals(PlotDieFace.OPPORTUNITY_2, PlotDieFace.byD6Value[5])
    }

    @Test
    fun `damage roller sums dice and bonus`() {
        val result = DamageRoller(Random(7)).roll(Die.D6, count = 3, flatBonus = 2)
        assertEquals(3, result.rolls.size)
        assertTrue(result.rolls.all { it in 1..6 })
        assertEquals(result.rolls.sum() + 2, result.total)
    }

    @Test
    fun `die rolls stay within bounds`() {
        val random = Random(99)
        Die.entries.forEach { die ->
            repeat(200) {
                val value = die.roll(random)
                assertTrue(value in 1..die.sides, "$die rolled $value")
            }
        }
    }
}
