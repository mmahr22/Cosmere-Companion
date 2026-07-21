package com.cosmere.companion.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ItemTest {

    private fun item(weight: String?) = Item(
        id = "test",
        name = "Test Item",
        itemType = "equipment",
        weight = weight,
        summary = "",
    )

    @Test
    fun `weightLb parses plain and decimal values`() {
        assertEquals(3.0, item("3 lb.").weightLb)
        assertEquals(1.5, item("1.5 lb.").weightLb)
        assertEquals(0.01, item("0.01 lb.").weightLb)
    }

    @Test
    fun `weightLb takes the lower bound of a range and ignores qualifiers`() {
        assertEquals(0.5, item("0.5–10 lb.").weightLb)
        assertEquals(1.0, item("1 lb. each").weightLb)
        assertEquals(1.0, item("1 lb. (empty)").weightLb)
    }

    @Test
    fun `weightLb is zero for weightless, unlisted, or missing weight`() {
        assertEquals(0.0, item("Weightless").weightLb)
        assertEquals(0.0, item("—").weightLb)
        assertEquals(0.0, item(null).weightLb)
    }
}
