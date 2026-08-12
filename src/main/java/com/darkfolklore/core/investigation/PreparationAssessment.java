package com.darkfolklore.core.investigation;

import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.knowledge.lore.KnowledgeStage;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.traits.ItemTrait;
import com.darkfolklore.core.traits.TraitResolver;
import com.darkfolklore.core.weakness.WeaknessRule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Player-facing preparation is gated by learned lore, not by hidden weakness ground truth. */
public record PreparationAssessment(
        KnowledgeStage knowledgeStage,
        boolean hasKnownCountermeasure,
        boolean prepared,
        List<String> satisfiedRules,
        List<Set<ItemTrait>> missingOptions
) {
    public static PreparationAssessment evaluate(ServerPlayer player, InvestigationProfile profile) {
        KnowledgeStage stage = FolkloreSavedData.get(player.getServer())
                .lore(player.getUUID(), profile.concept()).stage();
        EnumSet<ItemTrait> inventory = EnumSet.noneOf(ItemTrait.class);
        for (ItemStack stack : player.getInventory().items) inventory.addAll(TraitResolver.itemTraits(stack));
        inventory.addAll(TraitResolver.itemTraits(player.getOffhandItem()));
        inventory.addAll(TraitResolver.itemTraits(player.getMainHandItem()));
        return evaluate(stage, inventory, profile, FolkloreDataManager.INSTANCE.weaknesses().rules());
    }

    static PreparationAssessment evaluate(KnowledgeStage stage, Set<ItemTrait> inventory,
                                          InvestigationProfile profile, Collection<WeaknessRule> rules) {
        if (stage.ordinal() < KnowledgeStage.STUDIED.ordinal()) {
            return new PreparationAssessment(stage, false, false, List.of(), List.of());
        }
        List<String> satisfied = new ArrayList<>();
        List<Set<ItemTrait>> missing = new ArrayList<>();
        for (WeaknessRule rule : rules) {
            if (!profile.creatureTraits().containsAll(rule.targetTraits())) continue;
            if (inventory.containsAll(rule.requiredItemTraits())) satisfied.add(rule.id());
            else missing.add(rule.requiredItemTraits());
        }
        boolean known = !satisfied.isEmpty() || !missing.isEmpty();
        return new PreparationAssessment(stage, known, known && !satisfied.isEmpty(),
                List.copyOf(satisfied), List.copyOf(missing));
    }
}
