package com.cosmere.companion.core.data

import com.cosmere.companion.core.model.ItemType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ItemDataTest {

    @Test
    fun `items load with unique ids and non-blank names and summaries`() {
        val items = RulesRepository.items
        assertTrue(items.isNotEmpty())
        val ids = items.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate item ids")
        items.forEach {
            assertTrue(it.name.isNotBlank(), "${it.id} has a blank name")
            assertTrue(it.summary.isNotBlank(), "${it.id} has a blank summary")
        }
    }

    @Test
    fun `every item resolves to a known item type`() {
        RulesRepository.items.forEach { item ->
            assertTrue(
                item.itemType in setOf("weapon", "armor", "equipment", "fabrial"),
                "${item.id} has unrecognized itemType '${item.itemType}'",
            )
        }
    }

    @Test
    fun `weapons carry damage and range, armor carries a deflect value`() {
        val weapons = RulesRepository.items.filter { it.type == ItemType.WEAPON }
        assertTrue(weapons.isNotEmpty())
        weapons.forEach {
            assertNotNull(it.damage, "${it.id} is missing damage")
            assertNotNull(it.range, "${it.id} is missing range")
        }

        val armor = RulesRepository.items.filter { it.type == ItemType.ARMOR }
        assertTrue(armor.isNotEmpty())
        armor.forEach {
            assertNotNull(it.deflectValue, "${it.id} is missing a deflect value")
        }
    }

    @Test
    fun `fabrials carry a charges value`() {
        val fabrials = RulesRepository.items.filter { it.type == ItemType.FABRIAL }
        assertTrue(fabrials.isNotEmpty())
        fabrials.forEach {
            assertNotNull(it.charges, "${it.id} is missing a charges value")
        }
    }

    @Test
    fun `itemById resolves known ids and returns null for unknown ones`() {
        assertNotNull(RulesRepository.itemById("shardblade"))
        assertNotNull(RulesRepository.itemById("soulcaster"))
        assertEquals(null, RulesRepository.itemById("nonexistent"))
    }
}
