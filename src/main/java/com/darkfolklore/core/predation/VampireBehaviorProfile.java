package com.darkfolklore.core.predation;

/**
 * Stable behavioral archetype used by Dark Folklore's predation director.
 *
 * <p>The profile never changes provider-owned vampire facts, infection chance, conversion, cure, inheritance,
 * or MCA vampire target/navigation. For wild Vampirism mobs it may influence Dark Folklore's bounded autonomous
 * hunt choice and post-feed aggression; for MCA vampires it is observational/narrative only.</p>
 */
public enum VampireBehaviorProfile {
    CONTROLLED,
    CAUTIOUS,
    PREDATOR,
    RIPPER,
    RECRUITER,
    VENGEFUL
}
