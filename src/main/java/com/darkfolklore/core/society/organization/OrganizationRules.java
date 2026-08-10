package com.darkfolklore.core.society.organization;

import com.darkfolklore.core.knowledge.social.SecretType;

import java.util.Optional;
import java.util.Set;

/** Pure recruitment and influence policy shared by runtime code and tests. */
public final class OrganizationRules {
    private OrganizationRules() {}

    public static Optional<OrganizationType> naturalAffiliation(Set<SecretType> factualSecrets) {
        if (factualSecrets.contains(SecretType.VAMPIRE)) return Optional.of(OrganizationType.VAMPIRE_COVEN);
        if (factualSecrets.contains(SecretType.WEREWOLF)) return Optional.of(OrganizationType.WEREWOLF_PACK);
        if (factualSecrets.contains(SecretType.WITCH)) return Optional.of(OrganizationType.WITCH_COVEN);
        if (factualSecrets.contains(SecretType.HUNTER)) return Optional.of(OrganizationType.HUNTER_SOCIETY);
        return Optional.empty();
    }

    /** Hunter societies are founded by village suspicion, not silently by one hunter loading. */
    public static boolean mayAutoFound(OrganizationType type) {
        return type != OrganizationType.HUNTER_SOCIETY;
    }

    public static int completedContractInfluence(OrganizationType type) {
        return type == OrganizationType.HUNTER_SOCIETY ? 4 : 1;
    }

    public static String generatedName(OrganizationType type, int regionX, int regionZ) {
        String label = switch (type) {
            case VAMPIRE_COVEN -> "Vampire Coven";
            case HUNTER_SOCIETY -> "Hunter Society";
            case WEREWOLF_PACK -> "Werewolf Pack";
            case WITCH_COVEN -> "Witch Coven";
        };
        return label + " " + regionX + "," + regionZ;
    }
}
