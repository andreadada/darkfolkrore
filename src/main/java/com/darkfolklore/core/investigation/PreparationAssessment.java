package com.darkfolklore.core.investigation;

import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.traits.ItemTrait;
import com.darkfolklore.core.traits.TraitResolver;
import com.darkfolklore.core.weakness.WeaknessRule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public record PreparationAssessment(
        boolean hasKnownCountermeasure,
        boolean prepared,
        List<String> satisfiedRules,
        List<Set<ItemTrait>> missingOptions
) {
    public static PreparationAssessment evaluate(ServerPlayer player, InvestigationProfile profile) {
        EnumSet<ItemTrait> inventory = EnumSet.noneOf(ItemTrait.class);
        for (ItemStack stack : player.getInventory().items) inventory.addAll(TraitResolver.itemTraits(stack));
        inventory.addAll(TraitResolver.itemTraits(player.getOffhandItem()));

        List<String> satisfied = new ArrayList<>();
        List<Set<ItemTrait>> missing = new ArrayList<>();
        for (WeaknessRule rule : FolkloreDataManager.INSTANCE.weaknesses().rules()) {
            if (!profile.creatureTraits().containsAll(rule.targetTraits())) continue;
            if (inventory.containsAll(rule.requiredItemTraits())) {
                satisfied.add(rule.id());
            } else {
                missing.add(rule.requiredItemTraits());
            }
        }
        boolean known = !satisfied.isEmpty() || !missing.isEmpty();
        return new PreparationAssessment(known, !satisfied.isEmpty(),
                List.copyOf(satisfied), List.copyOf(missing));
    }
}
