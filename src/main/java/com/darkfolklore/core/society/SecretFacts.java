package com.darkfolklore.core.society;

import com.darkfolklore.core.knowledge.social.SecretType;
import com.darkfolklore.core.traits.CreatureTrait;
import com.darkfolklore.core.traits.TraitResolver;
import net.minecraft.world.entity.Entity;

import java.util.EnumSet;
import java.util.Set;

public final class SecretFacts {
    private SecretFacts() {}

    public static Set<SecretType> actualSecrets(Entity entity) {
        Set<CreatureTrait> traits = TraitResolver.creatureTraits(entity);
        EnumSet<SecretType> result = EnumSet.noneOf(SecretType.class);
        if (traits.contains(CreatureTrait.VAMPIRE)) result.add(SecretType.VAMPIRE);
        if (traits.contains(CreatureTrait.WEREWOLF)) result.add(SecretType.WEREWOLF);
        if (traits.contains(CreatureTrait.HUNTER)) result.add(SecretType.HUNTER);
        if (traits.contains(CreatureTrait.WITCH)) result.add(SecretType.WITCH);
        if (traits.contains(CreatureTrait.FAE)) result.add(SecretType.FAE_TOUCHED);
        if (!result.isEmpty()) result.add(SecretType.SUPERNATURAL_IDENTITY);
        return Set.copyOf(result);
    }

    public static String canonicalConcept(Entity entity) {
        Set<CreatureTrait> traits = TraitResolver.creatureTraits(entity);
        if (traits.contains(CreatureTrait.VAMPIRE)) return "darkfolklore:vampire";
        if (traits.contains(CreatureTrait.WEREWOLF)) return "darkfolklore:werewolf";
        if (traits.contains(CreatureTrait.FAE)) return "darkfolklore:fae";
        if (traits.contains(CreatureTrait.SPIRIT)) return "darkfolklore:spirit";
        if (traits.contains(CreatureTrait.UNDEAD)) return "darkfolklore:undead";
        return "darkfolklore:supernatural";
    }
}
