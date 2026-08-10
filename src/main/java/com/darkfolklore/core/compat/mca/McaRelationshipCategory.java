package com.darkfolklore.core.compat.mca;

/**
 * Relationship evidence exposed by MCA 7.7.32.
 *
 * <p>The directional names describe the rumor source relative to the observer. MCA does not expose a general
 * NPC-to-NPC friendship or enemy graph in this version, so those concepts are deliberately absent.</p>
 */
public enum McaRelationshipCategory {
    SELF,
    SPOUSE,
    SOURCE_IS_PARENT,
    SOURCE_IS_CHILD,
    SIBLING,
    PLAYER_FRIEND,
    PLAYER_BOUNTY_TARGET,
    STRANGER,
    UNKNOWN,
    NOT_APPLICABLE
}
