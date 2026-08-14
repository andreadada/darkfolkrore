package com.darkfolklore.core.living.casebook;

import com.darkfolklore.core.canonical.CanonicalDefinition;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.living.LivingFolkloreConfig;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.traits.ItemTrait;
import com.darkfolklore.core.traits.TraitResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import java.util.Optional;

public final class CasebookResearchBridge {
    public static final CasebookResearchBridge INSTANCE = new CasebookResearchBridge();
    private CasebookResearchBridge() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPickup(ItemEntityPickupEvent.Post event) {
        if (!LivingFolkloreConfig.LIVING_FOLKLORE.get() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        var stack = event.getOriginalStack();
        if (!TraitResolver.itemTraits(stack).contains(ItemTrait.ARCHAEOLOGICAL_LORE)) return;
        var assignment = FolkloreSavedData.get(player.getServer()).activeContract(player.getUUID()).orElse(null);
        if (assignment == null) return;
        var concept = FolkloreDataManager.INSTANCE.canonical().resolve(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).map(CanonicalDefinition::concept);
        if (concept.isEmpty() || !concept.get().equals(assignment.contract().targetConcept())) return;
        var data = LivingFolkloreSavedData.get(player.getServer());
        var record = data.caseForContract(assignment.contract().id()).orElse(null);
        if (record == null) return;
        record.addNote(new CaseNote(player.level().getGameTime(), CaseNoteKind.ARCHAEOLOGY,
                "A relevant archaeological find supports historical lore, but does not prove the current culprit.",
                Optional.empty(), Optional.empty(), Optional.empty(), 0.6F), LivingFolkloreConfig.CASE_NOTES.get());
        data.changed(record);
    }
}
