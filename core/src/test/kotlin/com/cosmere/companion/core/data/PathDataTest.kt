package com.cosmere.companion.core.data

import com.cosmere.companion.core.model.Activation
import com.cosmere.companion.core.model.Skill
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PathDataTest {

    @Test
    fun `agent path loads with three specialties`() {
        val agent = assertNotNull(RulesRepository.pathById("agent"))
        assertEquals(listOf("Investigator", "Spy", "Thief"), agent.specialties)
        assertEquals("INSIGHT", agent.startingSkillId)
        // Starting skill id must be a real skill.
        assertNotNull(Skill.valueOf(agent.startingSkillId!!))
    }

    @Test
    fun `agent talent tree has a key talent plus eight per specialty`() {
        val talents = RulesRepository.talentsForPath("agent")
        assertEquals(25, talents.size)
        assertEquals(1, talents.count { it.isKey })
        listOf("Investigator", "Spy", "Thief").forEach { specialty ->
            assertEquals(8, talents.count { it.specialty == specialty }, "talents in $specialty")
        }
    }

    @Test
    fun `every path has exactly one key talent and unique talent ids`() {
        RulesRepository.paths.forEach { path ->
            val talents = RulesRepository.talentsForPath(path.id)
            assertEquals(1, talents.count { it.isKey }, "key talents in ${path.id}")
            assertEquals(path.keyTalentId, talents.first { it.isKey }.id)
            talents.filterNot { it.isKey }.forEach { talent ->
                assertTrue(
                    talent.specialty in path.specialties,
                    "${talent.id} has unknown specialty ${talent.specialty}",
                )
            }
        }
        val ids = RulesRepository.talents.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate talent ids")
    }

    @Test
    fun `talent prerequisites reference real talents and skills`() {
        val ids = RulesRepository.talents.map { it.id }.toSet()
        RulesRepository.talents.forEach { talent ->
            talent.prerequisiteTalents.forEach { prereq ->
                assertTrue(prereq in ids, "${talent.id} references unknown talent $prereq")
            }
            val validSkillIds =
                Skill.entries.map { it.name }.toSet() +
                    com.cosmere.companion.core.model.Surge.entries.map { it.name }.toSet()
            talent.prerequisiteSkills.keys.forEach { skillId ->
                assertTrue(skillId in validSkillIds, "${talent.id} references unknown skill $skillId")
            }
        }
    }

    @Test
    fun `every non-key talent traces back to its path's key talent`() {
        val byId = RulesRepository.talents.associateBy { it.id }
        RulesRepository.talents.filterNot { it.isKey }.forEach { talent ->
            var frontier = listOf(talent)
            var foundKey = false
            val seen = mutableSetOf<String>()
            while (frontier.isNotEmpty() && !foundKey) {
                frontier = frontier
                    .flatMap { it.prerequisiteTalents }
                    .filter(seen::add)
                    .mapNotNull(byId::get)
                foundKey = frontier.any { it.isKey }
            }
            assertTrue(foundKey, "${talent.id} does not chain to a key talent")
        }
    }

    @Test
    fun `activation strings all map to a type`() {
        RulesRepository.talents.forEach { talent ->
            // Throws if unmappable; PASSIVE is the explicit fallback only for "passive".
            val type = talent.activationType
            if (talent.activation == "passive") {
                assertEquals(Activation.PASSIVE, type)
            }
        }
        val known = setOf("action1", "action2", "action3", "free", "reaction", "special", "passive")
        RulesRepository.talents.forEach {
            assertTrue(it.activation in known, "${it.id} has unknown activation '${it.activation}'")
        }
    }
}
