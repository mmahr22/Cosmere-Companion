package com.cosmere.companion.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SingerFormDataTest {

    @Test
    fun `fourteen forms load with unique ids`() {
        assertEquals(14, RulesRepository.singerForms.size)
        val ids = RulesRepository.singerForms.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate form ids")
    }

    @Test
    fun `dullform and mateform are the only forms with no granting talent`() {
        val starting = RulesRepository.singerForms.filter { it.grantedByTalentId == null }
        assertEquals(setOf("dullform", "mateform"), starting.map { it.id }.toSet())
    }

    @Test
    fun `every non-starting form is granted by a real talent in the singer tree`() {
        val singerTalentIds = RulesRepository.talentsForPath("singer").map { it.id }.toSet()
        RulesRepository.singerForms
            .mapNotNull { it.grantedByTalentId }
            .forEach { talentId ->
                assertTrue(talentId in singerTalentIds, "$talentId is not a singer-tree talent")
            }
    }

    @Test
    fun `voidform-bonded forms are exactly the six granted by post-ambitious-mind talents`() {
        val voidformGrantingTalents = setOf("forms_of_destruction", "forms_of_expansion", "forms_of_mystery")
        val voidforms = RulesRepository.singerForms.filter { it.voidform }
        assertEquals(6, voidforms.size)
        voidforms.forEach { form ->
            assertTrue(form.grantedByTalentId in voidformGrantingTalents, "${form.id} unexpectedly marked voidform")
        }
    }

    @Test
    fun `singerFormById resolves known ids and returns null for unknown ones`() {
        assertNotNull(RulesRepository.singerFormById("stormform"))
        assertEquals(null, RulesRepository.singerFormById("nonexistent"))
    }
}
