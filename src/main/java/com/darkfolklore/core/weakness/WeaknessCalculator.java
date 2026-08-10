package com.darkfolklore.core.weakness;

import com.darkfolklore.core.traits.CreatureTrait;
import com.darkfolklore.core.traits.ItemTrait;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class WeaknessCalculator {
    private WeaknessCalculator() {}

    public static Result calculate(float baseDamage, Set<CreatureTrait> creatureTraits,
                                   Set<ItemTrait> itemTraits, String targetNamespace,
                                   List<WeaknessRule> rules) {
        float multiplier = 1.0F;
        String appliedRule = "";
        for (WeaknessRule rule : rules.stream()
                .sorted(Comparator.comparingInt(WeaknessRule::priority).reversed())
                .toList()) {
            if (rule.matches(creatureTraits, itemTraits, targetNamespace)) {
                multiplier = rule.multiplier();
                appliedRule = rule.id();
                break;
            }
        }
        return new Result(baseDamage, baseDamage * multiplier, multiplier, appliedRule);
    }

    public record Result(float baseDamage, float finalDamage, float multiplier, String appliedRule) {
        public boolean modified() {
            return Float.compare(multiplier, 1.0F) != 0;
        }
    }
}
