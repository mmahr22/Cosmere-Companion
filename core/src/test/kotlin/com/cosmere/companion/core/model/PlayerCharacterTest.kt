package com.cosmere.companion.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerCharacterTest {

    private val attributes = mapOf(
        Attribute.STRENGTH to 2,
        Attribute.SPEED to 3,
        Attribute.INTELLECT to 1,
        Attribute.WILLPOWER to 2,
        Attribute.AWARENESS to 3,
        Attribute.PRESENCE to 1,
    )

    @Test
    fun `defaults current health and focus to the computed max at creation`() {
        val character = PlayerCharacter(
            name = "Kaladin",
            attributes = attributes,
            heroicPathId = "warrior",
        )

        assertEquals(character.maxHealth, character.currentHealth)
        assertEquals(character.maxFocus, character.currentFocus)
        assertEquals(12, character.currentHealth) // 10 + STR 2
        assertEquals(4, character.currentFocus) // 2 + WIL 2
    }

    @Test
    fun `defense and attribute lookups delegate to CharacterMath`() {
        val character = PlayerCharacter(
            name = "Shallan",
            attributes = attributes,
            heroicPathId = "scholar",
        )

        assertEquals(2, character.attribute(Attribute.STRENGTH))
        assertEquals(3, character.attribute(Attribute.SPEED))
        assertEquals(
            CharacterMath.defense(Defense.PHYSICAL, attributes),
            character.defense(Defense.PHYSICAL),
        )
    }

    @Test
    fun `skill rank looks up by id and defaults to zero`() {
        val character = PlayerCharacter(
            name = "Dalinar",
            attributes = attributes,
            heroicPathId = "leader",
            skillRanks = mapOf(Skill.LEADERSHIP.name to 2, "adhesion" to 1),
        )

        assertEquals(2, character.skillRank(Skill.LEADERSHIP.name))
        assertEquals(1, character.skillRank("adhesion"))
        assertEquals(0, character.skillRank(Skill.STEALTH.name))
    }

    @Test
    fun `explicit current health and focus override the computed defaults`() {
        val character = PlayerCharacter(
            name = "Injured Bridgeman",
            attributes = attributes,
            heroicPathId = "warrior",
            currentHealth = 3,
            currentFocus = 1,
        )

        assertEquals(3, character.currentHealth)
        assertEquals(1, character.currentFocus)
        assertEquals(12, character.maxHealth)
    }
}
