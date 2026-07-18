package com.cosmere.companion.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterMathTest {

    @Test
    fun `defenses use the book's worked examples`() {
        // Physical: STR 2, SPD 3 -> 15
        assertEquals(
            15,
            CharacterMath.defense(
                Defense.PHYSICAL,
                mapOf(Attribute.STRENGTH to 2, Attribute.SPEED to 3),
            ),
        )
        // Cognitive: INT 2, WIL 2 -> 14
        assertEquals(
            14,
            CharacterMath.defense(
                Defense.COGNITIVE,
                mapOf(Attribute.INTELLECT to 2, Attribute.WILLPOWER to 2),
            ),
        )
        // Spiritual: AWA 3, PRE 0 -> 13
        assertEquals(
            13,
            CharacterMath.defense(
                Defense.SPIRITUAL,
                mapOf(Attribute.AWARENESS to 3, Attribute.PRESENCE to 0),
            ),
        )
    }

    @Test
    fun `focus matches the book's worked example`() {
        // WIL 2 -> 4
        assertEquals(4, CharacterMath.maxFocus(2))
    }

    @Test
    fun `level 1 health is 10 plus strength`() {
        assertEquals(12, CharacterMath.maxHealth(1, strength = 2))
        assertEquals(10, CharacterMath.maxHealth(1, strength = 0))
    }

    @Test
    fun `health follows the advancement table gains`() {
        val str = 2
        // Levels 2-5 add +5 each.
        assertEquals(12 + 5 * 4, CharacterMath.maxHealth(5, str))
        // Level 6 adds +4 plus STR again.
        assertEquals(12 + 20 + 4 + str, CharacterMath.maxHealth(6, str))
        // Levels 7-10 add +4 each.
        assertEquals(12 + 20 + 4 + str + 4 * 4, CharacterMath.maxHealth(10, str))
        // Level 11 adds +3 plus STR.
        assertEquals(CharacterMath.maxHealth(10, str) + 3 + str, CharacterMath.maxHealth(11, str))
        // Level 16 adds +2 plus STR.
        assertEquals(CharacterMath.maxHealth(15, str) + 2 + str, CharacterMath.maxHealth(16, str))
        // Level 21+ adds +1 per level.
        assertEquals(CharacterMath.maxHealth(20, str) + 1, CharacterMath.maxHealth(21, str))
    }

    @Test
    fun `tiers span five levels each`() {
        assertEquals(1, CharacterMath.tier(1))
        assertEquals(1, CharacterMath.tier(5))
        assertEquals(2, CharacterMath.tier(6))
        assertEquals(3, CharacterMath.tier(11))
        assertEquals(4, CharacterMath.tier(16))
        assertEquals(5, CharacterMath.tier(21))
        assertEquals(5, CharacterMath.tier(30))
    }

    @Test
    fun `max skill rank climbs with tier and caps at 5`() {
        assertEquals(2, CharacterMath.maxSkillRank(1))
        assertEquals(3, CharacterMath.maxSkillRank(6))
        assertEquals(4, CharacterMath.maxSkillRank(11))
        assertEquals(5, CharacterMath.maxSkillRank(16))
        assertEquals(5, CharacterMath.maxSkillRank(25))
    }

    @Test
    fun `unverified movement anchors match worked examples`() {
        // Speed 3 -> 30 ft and Speed 5 -> 40 ft appear in printed examples.
        assertEquals(30, UnverifiedDerivedStats.movementFeet(3))
        assertEquals(40, UnverifiedDerivedStats.movementFeet(5))
    }

    @Test
    fun `unverified recovery die anchor matches worked example`() {
        // Willpower 2 -> d6 appears in a printed example.
        assertEquals(Die.D6, UnverifiedDerivedStats.recoveryDie(2))
    }

    @Test
    fun `unverified senses range anchor matches worked example`() {
        // Awareness 3 -> 20 ft appears in a printed example.
        assertEquals(20, UnverifiedDerivedStats.sensesRangeFeet(3))
    }
}
