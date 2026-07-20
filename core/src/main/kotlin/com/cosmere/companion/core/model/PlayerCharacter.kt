package com.cosmere.companion.core.model

import com.cosmere.companion.core.data.RulesRepository

/**
 * A player character sheet: identity, attributes, path choices, skill ranks,
 * and the resource pools tracked during play.
 *
 * Skill ranks are keyed by [Skill.name] for the eighteen standard skills and
 * by a surge's id (see [SurgeEntry.id]) once a Radiant order's surges are
 * unlocked.
 *
 * [ancestryId] and [cultureIds] resolve against [com.cosmere.companion.core.data.RulesRepository.ancestryById]
 * and [com.cosmere.companion.core.data.RulesRepository.cultureById]; a
 * character has at most two cultures per the book's character creation
 * rules. Neither field grants talents automatically — like heroic/Radiant
 * path talents, ancestry bonus talents aren't tracked as a structured list
 * anywhere in this app yet.
 *
 * [unlockedFormIds] and [currentFormId] track a Singer's Change Form talent
 * tree: which forms (beyond the always-available dullform/mateform) the
 * player has picked up via later Singer-tree talents, and which one is
 * currently active. Since individual talent purchases aren't tracked
 * anywhere else in this app either, [unlockedFormIds] is a direct player
 * declaration rather than something derived from a talent list — the sheet
 * lets the player toggle it the same way it lets them edit skill ranks
 * directly rather than modeling the talents that grant them.
 *
 * There's no printed formula for maximum Investiture in the data bundled so
 * far (unlike health/focus, which [CharacterMath] derives directly from the
 * book's worked examples), so [maxInvestiture] is tracked as a plain value
 * the player sets rather than a computed one.
 *
 * [bonusAttributePoints] and [bonusSkillPoints] cover ad-hoc GM grants (story
 * rewards, homebrew boons) that fall outside the normal level-based budget
 * ([totalAttributePoints]/[totalSkillRanks]) — they add directly to that
 * budget rather than requiring a full level-up, which would also change
 * skill/attribute points together and recompute max health/focus.
 *
 * [inventory] is every item the character owns, keyed by [Item.id] with a
 * quantity; [equippedArmorId] and [equippedWeaponIds] mark which of those
 * owned items are currently worn/wielded (the book only allows one worn
 * armor at a time, but any number of held weapons). Only [equippedArmorId]
 * feeds into a derived stat ([deflectValue]) today — deflect is the one
 * piece of gear math this app tracks outside of combat, since everything
 * else an item's traits do (damage, special actions) only matters mid-fight.
 */
data class PlayerCharacter(
    // 0 means "not yet persisted" — Room assigns a real autoGenerate id on first save.
    val id: Int = 0,
    val name: String,
    val ancestryId: String? = null,
    val cultureIds: List<String> = emptyList(),
    val level: Int = 1,
    val attributes: Map<Attribute, Int> = Attribute.entries.associateWith { 0 },
    val heroicPathId: String,
    val specialty: String? = null,
    val radiantPathId: String? = null,
    val skillRanks: Map<String, Int> = emptyMap(),
    val currentHealth: Int = CharacterMath.maxHealth(level, attributes[Attribute.STRENGTH] ?: 0),
    val currentFocus: Int = CharacterMath.maxFocus(attributes[Attribute.WILLPOWER] ?: 0),
    val currentInvestiture: Int = 0,
    val maxInvestiture: Int = 0,
    val unlockedFormIds: List<String> = emptyList(),
    val currentFormId: String? = null,
    val inventory: Map<String, Int> = emptyMap(),
    val equippedWeaponIds: List<String> = emptyList(),
    val equippedArmorId: String? = null,
    val avatarPath: String? = null,
    val bonusAttributePoints: Int = 0,
    val bonusSkillPoints: Int = 0,
) {
    fun attribute(attribute: Attribute): Int = attributes[attribute] ?: 0

    /** [attribute] plus any bonus from the character's currently active Singer form, if any. */
    fun effectiveAttribute(attribute: Attribute): Int {
        val formBonus = currentFormId
            ?.let { RulesRepository.singerFormById(it) }
            ?.attributeBonuses
            ?.get(attribute.name)
            ?: 0
        return attribute(attribute) + formBonus
    }

    fun defense(defense: Defense): Int =
        CharacterMath.defense(defense, Attribute.entries.associateWith { effectiveAttribute(it) })

    val maxHealth: Int get() = CharacterMath.maxHealth(level, effectiveAttribute(Attribute.STRENGTH))

    val maxFocus: Int get() = CharacterMath.maxFocus(effectiveAttribute(Attribute.WILLPOWER))

    fun skillRank(skillId: String): Int = skillRanks[skillId] ?: 0

    /** Attribute point budget for [level], plus any ad-hoc [bonusAttributePoints] a GM has granted. */
    val totalAttributePoints: Int get() = CharacterMath.totalAttributePoints(level) + bonusAttributePoints

    /** Skill rank budget for [level] (including the free path-starting rank), plus any [bonusSkillPoints]. */
    val totalSkillRanks: Int get() = CharacterMath.totalSkillRanks(level) + bonusSkillPoints

    /** Forms this character can currently switch into: the two free starting forms plus any unlocked. */
    val availableFormIds: List<String> get() = listOf("dullform", "mateform") + unlockedFormIds

    fun inventoryQuantity(itemId: String): Int = inventory[itemId] ?: 0

    /** Deflect value from the currently worn armor, or 0 if none is equipped. */
    val deflectValue: Int
        get() = equippedArmorId?.let { RulesRepository.itemById(it)?.deflectValue } ?: 0
}
