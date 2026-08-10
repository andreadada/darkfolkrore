package com.darkfolklore.core.weakness;

import com.darkfolklore.core.traits.CreatureTrait;
import com.darkfolklore.core.traits.ItemTrait;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WeaknessCalculatorTest {
    private static final WeaknessRule SILVER = new WeaknessRule(
            "darkfolklore:silver_vs_werewolf", Set.of(CreatureTrait.WEREWOLF),
            Set.of(ItemTrait.SILVER_WEAPON), 1.75F, Set.of("werewolves"), 100);

    @Test
    void appliesSemanticWeaknessToForeignWerewolf() {
        WeaknessCalculator.Result result = WeaknessCalculator.calculate(8.0F,
                Set.of(CreatureTrait.WEREWOLF), Set.of(ItemTrait.SILVER_WEAPON),
                "fangs_n_claws", List.of(SILVER));
        assertEquals(14.0F, result.finalDamage());
        assertTrue(result.modified());
    }

    @Test
    void avoidsNativeProviderDoubleApplication() {
        WeaknessCalculator.Result result = WeaknessCalculator.calculate(8.0F,
                Set.of(CreatureTrait.WEREWOLF), Set.of(ItemTrait.SILVER_WEAPON),
                "werewolves", List.of(SILVER));
        assertEquals(8.0F, result.finalDamage());
        assertFalse(result.modified());
    }
}
