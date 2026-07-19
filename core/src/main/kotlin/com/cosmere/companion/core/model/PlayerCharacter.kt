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
 */
data class PlayerCharacter(
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

    /** Forms this character can currently switch into: the two free starting forms plus any unlocked. */
    val availableFormIds: List<String> get() = listOf("dullform", "mateform") + unlockedFormIds
}
