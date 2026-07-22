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

    @Test
    fun `available forms are the two starting forms plus any unlocked`() {
        val character = PlayerCharacter(
            name = "Rlain",
            ancestryId = "singer",
            attributes = attributes,
            heroicPathId = "warrior",
            unlockedFormIds = listOf("warform"),
        )

        assertEquals(listOf("dullform", "mateform", "warform"), character.availableFormIds)
    }

    @Test
    fun `active form's attribute bonuses feed into effective attribute, defense, and max health-focus`() {
        val noForm = PlayerCharacter(
            name = "Rlain",
            ancestryId = "singer",
            attributes = attributes,
            heroicPathId = "warrior",
            unlockedFormIds = listOf("warform"),
        )
        val inWarform = noForm.copy(currentFormId = "warform")

        // warform grants Strength +1
        assertEquals(2, noForm.effectiveAttribute(Attribute.STRENGTH))
        assertEquals(3, inWarform.effectiveAttribute(Attribute.STRENGTH))
        assertEquals(noForm.defense(Defense.PHYSICAL) + 1, inWarform.defense(Defense.PHYSICAL))
        assertEquals(noForm.maxHealth + 1, inWarform.maxHealth)

        // Attributes untouched by the active form are unaffected.
        assertEquals(noForm.effectiveAttribute(Attribute.WILLPOWER), inWarform.effectiveAttribute(Attribute.WILLPOWER))
        assertEquals(noForm.maxFocus, inWarform.maxFocus)
    }

    @Test
    fun `inventoryQuantity defaults to zero for items not carried`() {
        val character = PlayerCharacter(
            name = "Lopen",
            attributes = attributes,
            heroicPathId = "warrior",
            inventory = mapOf("rope" to 2),
        )

        assertEquals(2, character.inventoryQuantity("rope"))
        assertEquals(0, character.inventoryQuantity("longsword"))
    }

    @Test
    fun `deflect value comes from equipped armor and is zero when unarmored`() {
        val unarmored = PlayerCharacter(
            name = "Lopen",
            attributes = attributes,
            heroicPathId = "warrior",
            inventory = mapOf("chain_armor" to 1),
        )
        assertEquals(0, unarmored.deflectValue)

        val armored = unarmored.copy(equippedArmorId = "chain_armor")
        assertEquals(2, armored.deflectValue)
    }

    @Test
    fun `GM bonus points add on top of the level-derived budget`() {
        val character = PlayerCharacter(
            name = "Lopen",
            attributes = attributes,
            heroicPathId = "warrior",
        )

        assertEquals(12, character.totalAttributePoints)
        assertEquals(5, character.totalSkillRanks)

        val rewarded = character.copy(bonusAttributePoints = 2, bonusSkillPoints = 3)
        assertEquals(14, rewarded.totalAttributePoints)
        assertEquals(8, rewarded.totalSkillRanks)
    }

    @Test
    fun `spoken ideal is zero without a radiant path and derives from purchased ideal talents otherwise`() {
        val nonRadiant = PlayerCharacter(name = "Kaladin", attributes = attributes, heroicPathId = "warrior")
        assertEquals(0, nonRadiant.spokenIdeal)

        val firstIdeal = nonRadiant.copy(
            radiantPathId = "windrunner",
            purchasedTalentIds = listOf("first_ideal_windrunner"),
        )
        assertEquals(1, firstIdeal.spokenIdeal)

        val thirdIdeal = firstIdeal.copy(
            purchasedTalentIds = listOf("first_ideal_windrunner", "second_ideal_windrunner", "third_ideal_windrunner"),
        )
        assertEquals(3, thirdIdeal.spokenIdeal)
    }

    @Test
    fun `total talent points use the character's ancestry bonus levels, plus any GM bonus`() {
        val human = PlayerCharacter(
            name = "Kaladin", ancestryId = "human", attributes = attributes, heroicPathId = "warrior", level = 6,
        )
        val singer = PlayerCharacter(
            name = "Rlain", ancestryId = "singer", attributes = attributes, heroicPathId = "warrior", level = 6,
        )
        // Humans get a bonus pick at level 1; singers' level-1 pick is the forced Change Form key talent instead.
        assertEquals(human.totalTalentPoints, singer.totalTalentPoints + 1)

        val bonused = human.copy(bonusTalentPoints = 2)
        assertEquals(human.totalTalentPoints + 2, bonused.totalTalentPoints)
    }

    @Test
    fun `accessible path ids start with the granted paths and grow as key talents are purchased`() {
        val character = PlayerCharacter(
            name = "Shallan",
            attributes = attributes,
            heroicPathId = "scholar",
            radiantPathId = "lightweaver",
            purchasedTalentIds = listOf("erudition", "first_ideal_lightweaver"),
        )
        assertEquals(setOf("scholar", "lightweaver"), character.accessiblePathIds.toSet())

        // Buying another heroic path's key talent (multi-pathing) opens that path's tree too.
        val multiPathed = character.copy(
            purchasedTalentIds = character.purchasedTalentIds + "vigilant_stance",
        )
        assertEquals(setOf("scholar", "lightweaver", "warrior"), multiPathed.accessiblePathIds.toSet())
    }

    @Test
    fun `carried weight sums inventory item weight times quantity`() {
        val character = PlayerCharacter(
            name = "Lopen",
            attributes = attributes,
            heroicPathId = "warrior",
            inventory = mapOf("knife" to 2, "longsword" to 1),
        )

        // knife 1 lb. x2 + longsword 3 lb. x1
        assertEquals(5.0, character.carriedWeightLb)
    }

    @Test
    fun `carrying capacity scales with effective Strength and flags when exceeded`() {
        val character = PlayerCharacter(
            name = "Lopen",
            attributes = attributes, // STRENGTH 2 -> 100 lb. capacity
            heroicPathId = "warrior",
        )
        assertEquals(100, character.carryingCapacityLb)
        assertEquals(false, character.isOverCarryingCapacity)

        val overloaded = character.copy(inventory = mapOf("longsword" to 50))
        assertEquals(true, overloaded.isOverCarryingCapacity)
    }

    @Test
    fun `wielding limit allows two one-handed weapons or a single Two-Handed weapon`() {
        val unarmed = PlayerCharacter(name = "Kaladin", attributes = attributes, heroicPathId = "warrior")
        assertEquals(true, unarmed.canWieldAdditionalWeapon("knife"))
        assertEquals(true, unarmed.canWieldAdditionalWeapon("longsword")) // Two-Handed, but nothing else equipped yet

        val oneHandWielded = unarmed.copy(equippedWeaponIds = listOf("knife"))
        assertEquals(true, oneHandWielded.canWieldAdditionalWeapon("shield")) // 2nd one-handed weapon: fine
        assertEquals(false, oneHandWielded.canWieldAdditionalWeapon("longsword")) // can't add a Two-Handed on top

        val twoWielded = unarmed.copy(equippedWeaponIds = listOf("knife", "shield"))
        assertEquals(false, twoWielded.canWieldAdditionalWeapon("rapier")) // already at the limit of two
        assertEquals(true, twoWielded.canWieldAdditionalWeapon("knife")) // already-equipped: unwielding is always fine

        val twoHandWielded = unarmed.copy(equippedWeaponIds = listOf("longsword"))
        assertEquals(false, twoHandWielded.canWieldAdditionalWeapon("knife")) // can't add anything alongside a Two-Handed weapon
    }
}
