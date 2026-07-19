package com.cosmere.companion.core.model

import kotlinx.serialization.Serializable

enum class ItemType {
    WEAPON,
    ARMOR,
    EQUIPMENT,
    FABRIAL,
}

/**
 * A piece of gear from the Items chapter: a weapon, armor, general equipment,
 * or fabrial. One flat shape covers all four since the book's own tables
 * differ only in which columns they use — [damage]/[range]/[skill] are
 * weapon-only, [deflectValue] is armor-only, and [charges] is fabrial-only —
 * while [traits]/[expertTraits]/[weight]/[price]/[summary] apply broadly.
 */
@Serializable
data class Item(
    val id: String,
    val name: String,
    val itemType: String,
    val category: String? = null,
    val skill: String? = null,
    val damage: String? = null,
    val range: String? = null,
    val deflectValue: Int? = null,
    val traits: List<String> = emptyList(),
    val expertTraits: List<String> = emptyList(),
    val charges: String? = null,
    val weight: String? = null,
    val price: String? = null,
    val summary: String,
    val page: Int? = null,
) {
    val type: ItemType
        get() = when (itemType.lowercase()) {
            "weapon" -> ItemType.WEAPON
            "armor" -> ItemType.ARMOR
            "fabrial" -> ItemType.FABRIAL
            else -> ItemType.EQUIPMENT
        }
}

@Serializable
internal data class ItemsFile(val items: List<Item>)
