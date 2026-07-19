package com.cosmere.companion.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AncestryCultureDataTest {

    @Test
    fun `human and singer ancestries are both present`() {
        assertEquals(2, RulesRepository.ancestries.size)
        assertNotNull(RulesRepository.ancestryById("human"))
        assertNotNull(RulesRepository.ancestryById("singer"))
    }

    @Test
    fun `singer ancestry key talent resolves to a real key talent in the singer tree`() {
        val singer = assertNotNull(RulesRepository.ancestryById("singer"))
        val keyTalentId = assertNotNull(singer.keyTalentId)
        val keyTalent = assertNotNull(RulesRepository.talents.firstOrNull { it.id == keyTalentId })
        assertTrue(keyTalent.isKey)
        assertEquals("singer", keyTalent.pathId)
    }

    @Test
    fun `human ancestry has no forced key talent`() {
        val human = assertNotNull(RulesRepository.ancestryById("human"))
        assertEquals(null, human.keyTalentId)
    }

    @Test
    fun `thirteen cultures load with unique ids and non-blank expertise summaries`() {
        assertEquals(13, RulesRepository.cultures.size)
        val ids = RulesRepository.cultures.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate culture ids")
        RulesRepository.cultures.forEach { culture ->
            assertTrue(culture.expertiseSummary.isNotBlank(), "${culture.id} has a blank expertise summary")
        }
    }

    @Test
    fun `only the listener culture is singer-only`() {
        val singerOnly = RulesRepository.cultures.filter { it.singerOnly }
        assertEquals(listOf("listener"), singerOnly.map { it.id })
    }

    @Test
    fun `singer talent tree has one key talent and every other talent chains to it`() {
        val singerTalents = RulesRepository.talentsForPath("singer")
        assertEquals(8, singerTalents.size)
        assertEquals(1, singerTalents.count { it.isKey })
    }
}
