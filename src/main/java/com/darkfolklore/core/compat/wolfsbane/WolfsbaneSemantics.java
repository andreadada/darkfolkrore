package com.darkfolklore.core.compat.wolfsbane;

import java.util.Optional;

/** Pure, version-pinned Wolfsbane identity rules shared by runtime code and tests. */
public final class WolfsbaneSemantics {
    public static final String WEREWOLVES_VERSION = "2.0.3.3";
    public static final String ENCHANTED_VERSION = "4.2.7";

    public static final String CONCEPT = "darkfolklore:wolfsbane";
    public static final String CANONICAL_ITEM = "enchanted:wolfsbane_flower";
    public static final String CANONICAL_SEEDS = "enchanted:wolfsbane_seeds";
    public static final String CANONICAL_BLOCK = "enchanted:wolfsbane";
    public static final String LEGACY_ITEM = "werewolves:wolfsbane";
    public static final String LEGACY_BLOCK = "werewolves:wolfsbane";
    public static final String WEREWOLVES_EFFECT = "werewolves:wolfsbane";
    public static final String FINDER_ITEM = "werewolves:wolfsbane_finder";
    public static final String DIFFUSER_NORMAL_BLOCK = "werewolves:wolfsbane_diffuser_normal";
    public static final String DIFFUSER_LONG_BLOCK = "werewolves:wolfsbane_diffuser_long";
    public static final String DIFFUSER_IMPROVED_BLOCK = "werewolves:wolfsbane_diffuser_improved";
    public static final String DIFFUSER_BLOCK_ENTITY = "werewolves:wolfsbane_diffuser";

    private WolfsbaneSemantics() {}

    public static Identity classifyItem(String registryId) {
        if (registryId == null) {
            return Identity.UNRELATED;
        }
        return switch (registryId) {
            case CANONICAL_ITEM -> Identity.CANONICAL;
            case LEGACY_ITEM -> Identity.LEGACY;
            case CANONICAL_SEEDS -> Identity.SEEDS;
            default -> Identity.UNRELATED;
        };
    }

    /** Both flower items remain valid semantic recipe inputs; seeds do not. */
    public static boolean isRecipeIngredient(String registryId) {
        Identity identity = classifyItem(registryId);
        return identity == Identity.CANONICAL || identity == Identity.LEGACY;
    }

    public static Optional<String> futureLootReplacement(String registryId) {
        return LEGACY_ITEM.equals(registryId) ? Optional.of(CANONICAL_ITEM) : Optional.empty();
    }

    public static boolean supportsNativeBridge(String werewolvesVersion, String enchantedVersion) {
        return WEREWOLVES_VERSION.equals(werewolvesVersion) && ENCHANTED_VERSION.equals(enchantedVersion);
    }

    public enum Identity {
        CANONICAL,
        LEGACY,
        SEEDS,
        UNRELATED
    }
}
