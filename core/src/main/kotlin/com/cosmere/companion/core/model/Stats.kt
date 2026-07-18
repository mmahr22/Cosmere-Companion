package com.cosmere.companion.core.model

/**
 * The three defenses. Each is paired with two attributes that determine
 * its value, and each guards a corresponding resource pool.
 */
enum class Defense(val displayName: String) {
    PHYSICAL("Physical"),
    COGNITIVE("Cognitive"),
    SPIRITUAL("Spiritual");
}

/** The six character attributes, grouped in pairs under a defense. */
enum class Attribute(
    val displayName: String,
    val abbreviation: String,
    val defense: Defense,
) {
    STRENGTH("Strength", "STR", Defense.PHYSICAL),
    SPEED("Speed", "SPD", Defense.PHYSICAL),
    INTELLECT("Intellect", "INT", Defense.COGNITIVE),
    WILLPOWER("Willpower", "WIL", Defense.COGNITIVE),
    AWARENESS("Awareness", "AWA", Defense.SPIRITUAL),
    PRESENCE("Presence", "PRE", Defense.SPIRITUAL);
}

/** The three expendable resource pools tracked on the character sheet. */
enum class Resource(val displayName: String, val defense: Defense) {
    HEALTH("Health", Defense.PHYSICAL),
    FOCUS("Focus", Defense.COGNITIVE),
    INVESTITURE("Investiture", Defense.SPIRITUAL);
}

/** The eighteen standard skills and the attribute each one tests. */
enum class Skill(val displayName: String, val attribute: Attribute) {
    AGILITY("Agility", Attribute.SPEED),
    ATHLETICS("Athletics", Attribute.STRENGTH),
    CRAFTING("Crafting", Attribute.INTELLECT),
    DECEPTION("Deception", Attribute.PRESENCE),
    DEDUCTION("Deduction", Attribute.INTELLECT),
    DISCIPLINE("Discipline", Attribute.WILLPOWER),
    HEAVY_WEAPONRY("Heavy Weaponry", Attribute.STRENGTH),
    INSIGHT("Insight", Attribute.AWARENESS),
    INTIMIDATION("Intimidation", Attribute.WILLPOWER),
    LEADERSHIP("Leadership", Attribute.PRESENCE),
    LIGHT_WEAPONRY("Light Weaponry", Attribute.SPEED),
    LORE("Lore", Attribute.INTELLECT),
    MEDICINE("Medicine", Attribute.INTELLECT),
    PERCEPTION("Perception", Attribute.AWARENESS),
    PERSUASION("Persuasion", Attribute.PRESENCE),
    STEALTH("Stealth", Attribute.SPEED),
    SURVIVAL("Survival", Attribute.AWARENESS),
    THIEVERY("Thievery", Attribute.SPEED);

    companion object {
        fun forAttribute(attribute: Attribute): List<Skill> =
            entries.filter { it.attribute == attribute }
    }
}
