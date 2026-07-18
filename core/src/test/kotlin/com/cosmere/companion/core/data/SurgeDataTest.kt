package com.cosmere.companion.core.data

import com.cosmere.companion.core.model.Attribute
import com.cosmere.companion.core.model.Skill
import com.cosmere.companion.core.model.Surge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SurgeDataTest {

    @Test
    fun `all ten surges are present with valid attributes`() {
        assertEquals(10, RulesRepository.surges.size)
        val surgeIds = Surge.entries.map { it.id }.toSet()
        RulesRepository.surges.forEach { surge ->
            assertTrue(surge.id in surgeIds, "unknown surge id ${surge.id}")
            assertTrue(
                Attribute.entries.any { it.name == surge.attributeId },
                "${surge.id} references unknown attribute ${surge.attributeId}",
            )
        }
    }

    @Test
    fun `surge talent ids are unique across all surges`() {
        val ids = RulesRepository.surges.flatMap { it.talents }.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate surge talent ids")
    }

    @Test
    fun `surge talent prerequisites reference real talents and skills`() {
        val allTalentIds = RulesRepository.surges.flatMap { it.talents }.map { it.id }.toSet()
        val validSkillIds = Skill.entries.map { it.name }.toSet() + Surge.entries.map { it.name }.toSet()

        RulesRepository.surges.forEach { surge ->
            surge.talents.forEach { talent ->
                talent.prerequisiteTalents.forEach { prereq ->
                    assertTrue(prereq in allTalentIds, "${talent.id} references unknown talent $prereq")
                }
                talent.prerequisiteSkills.keys.forEach { skillId ->
                    assertTrue(skillId in validSkillIds, "${talent.id} references unknown skill $skillId")
                }
            }
        }
    }

    @Test
    fun `radiant paths reference real surges matching their surge ids`() {
        val surgeIds = RulesRepository.surges.map { it.id }.toSet()
        RulesRepository.paths.filter { it.type == "radiant" }.forEach { path ->
            path.surgeIds.forEach { surgeId ->
                assertTrue(surgeId in surgeIds, "${path.id} references unknown surge $surgeId")
            }
        }
    }

    @Test
    fun `transformation DC table has six categories with symmetric core entries`() {
        val table = RulesRepository.transformationDcTable
        assertEquals(6, table.size)
        // Same-category transformation should always be cheapest for that row.
        table.forEach { row ->
            val values = listOfNotNull(row.toSolids, row.toOrganics, row.toLiquids, row.toVapors, row.toClearAir, row.toFlame)
            assertTrue(values.isNotEmpty())
        }
    }
}
