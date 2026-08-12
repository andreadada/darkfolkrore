package com.darkfolklore.core.magic;

import java.util.Set;

public record MagicDisciplineProfile(MagicDiscipline discipline, String knowledgeConcept,
                                     Set<MagicUse> uses, Set<String> providerNamespaces) {
    public MagicDisciplineProfile {
        uses = Set.copyOf(uses);
        providerNamespaces = Set.copyOf(providerNamespaces);
    }
}
