package com.cosmere.companion.core.data

import com.cosmere.companion.core.model.Attribute
import com.cosmere.companion.core.model.Defense
import com.cosmere.companion.core.model.Skill
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RulesRepositoryTest {

    @Test
    fun `loads all fourteen conditions`() {
        assertEquals(14, RulesRepository.conditions.size)
        assertEquals(RulesRepository.conditions.map { it.name }.sorted(), RulesRepository.conditions.map { it.name })
    }

    @Test
    fun `bracketed and cumulative conditions are flagged`() {
        val exhausted = assertNotNull(RulesRepository.conditionById("exhausted"))
        assertTrue(exhausted.hasBracketValue)
        assertTrue(exhausted.cumulative)

        val slowed = assertNotNull(RulesRepository.conditionById("slowed"))
        assertTrue(!slowed.hasBracketValue)
        assertTrue(!slowed.cumulative)
    }

    @Test
    fun `six attributes pair evenly under three defenses`() {
        Defense.entries.forEach { defense ->
            assertEquals(2, Attribute.entries.count { it.defense == defense })
        }
    }

    @Test
    fun `eighteen skills match the character sheet distribution`() {
        assertEquals(18, Skill.entries.size)
        val expectedCounts = mapOf(
            Attribute.STRENGTH to 2,
            Attribute.SPEED to 4,
            Attribute.INTELLECT to 4,
            Attribute.WILLPOWER to 2,
            Attribute.AWARENESS to 3,
            Attribute.PRESENCE to 3,
        )
        expectedCounts.forEach { (attribute, count) ->
            assertEquals(count, Skill.forAttribute(attribute).size, "skills for $attribute")
        }
    }
}
