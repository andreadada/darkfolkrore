package com.darkfolklore.core.weakness;

import java.util.Collection;
import java.util.List;

public final class WeaknessRegistry {
    private volatile List<WeaknessRule> rules = List.of();

    public void replace(Collection<WeaknessRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<WeaknessRule> rules() {
        return rules;
    }
}
