package com.cosmere.companion.core.model

/**
 * The ten surges. When a Radiant completes their First Ideal goal, their
 * order's two surges are added to the character sheet as skills (starting
 * at 1 rank each) and advance like any other skill.
 */
enum class Surge(val displayName: String) {
    ADHESION("Adhesion"),
    ABRASION("Abrasion"),
    COHESION("Cohesion"),
    DIVISION("Division"),
    GRAVITATION("Gravitation"),
    ILLUMINATION("Illumination"),
    PROGRESSION("Progression"),
    TENSION("Tension"),
    TRANSFORMATION("Transformation"),
    TRANSPORTATION("Transportation");

    val id: String get() = name.lowercase()
}
