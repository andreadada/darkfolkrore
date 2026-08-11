package com.darkfolklore.core.compat.fieldguide;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.canonical.CanonicalDefinition;
import com.darkfolklore.core.canonical.CanonicalKind;
import com.darkfolklore.core.compat.FieldGuideBridge;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.knowledge.lore.KnowledgeStage;
import com.darkfolklore.core.knowledge.lore.LoreEngine;
import com.darkfolklore.core.knowledge.lore.LoreProgress;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.evandev.fieldguide.api.EntryUnlockData;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.LinkedHashSet;
import java.util.Set;

/** Exact 1.14.0 bridge. Field Guide remains binary; Dark Folklore owns tiered lore. */
public final class FieldGuideAdapter implements FieldGuideBridge {
    private static final int GUIDE_UNLOCK_THRESHOLD = KnowledgeStage.OBSERVED.threshold();
    private boolean runtimeAvailable = true;

    @SubscribeEvent
    public void onKill(LivingDeathEvent event) {
        if (!runtimeAvailable || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        String registryId = entityId.toString();
        String guideRegistryId = FolkloreDataManager.INSTANCE.canonical().resolve(registryId)
                .filter(definition -> definition.kind() == CanonicalKind.ENTITY)
                .map(definition -> definition.canonicalId().isBlank() ? registryId : definition.canonicalId())
                .orElse(registryId);
        try {
            PlayerFieldGuideProgress progress = FieldGuideProgressManager.getInstance().getProgress(player);
            if (progress != null) {
                progress.tryUnlock(player, entryId(guideRegistryId), "", EntryUnlockData.UnlockTrigger.KILL);
            }
        } catch (RuntimeException | LinkageError exception) {
            disableAfterFailure(exception);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!runtimeAvailable || !(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 100 != 0) return;
        try {
            FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();
            PlayerFieldGuideProgress progress = manager.getProgress(player);
            if (progress == null) return;
            FolkloreSavedData data = FolkloreSavedData.get(player.getServer());
            for (CanonicalDefinition definition : FolkloreDataManager.INSTANCE.canonical().definitions()) {
                if (definition.kind() != CanonicalKind.ENTITY) continue;
                LoreProgress lore = data.lore(player.getUUID(), definition.concept());
                boolean guideUnlocked = false;
                for (String implementation : entityImplementations(definition)) {
                    if (progress.isUnlocked(entryId(implementation))) {
                        guideUnlocked = true;
                        break;
                    }
                }

                if (guideUnlocked) {
                    if (lore.points() == 0) LoreEngine.INSTANCE.discoverOnce(player, definition.concept(), 10);
                } else if (lore.points() >= GUIDE_UNLOCK_THRESHOLD && !definition.canonicalId().isBlank()) {
                    ResourceLocation entry = entryId(definition.canonicalId());
                    if (manager.isValidEntry(entry) && progress.canUnlock(entry)) {
                        progress.unlock(player, entry, "", false);
                    }
                }
            }
        } catch (RuntimeException | LinkageError exception) {
            disableAfterFailure(exception);
        }
    }

    /**
     * Unlocks the implementation actually observed by an investigation. For
     * KEEP_DISTINCT concepts that exact provider entry is authoritative. For
     * canonical concepts whose runtime implementation has no dedicated guide
     * page (for example an internal provider variant), the bridge falls back to
     * the concept's canonical entity page instead of silently losing the unlock.
     */
    @Override
    public boolean unlockObservedImplementation(ServerPlayer player, String registryId) {
        if (!runtimeAvailable || registryId == null || registryId.isBlank()) return false;
        try {
            FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();
            PlayerFieldGuideProgress progress = manager.getProgress(player);
            if (progress == null) return false;

            if (unlockEntry(manager, progress, player, registryId)) return true;

            String canonicalId = FolkloreDataManager.INSTANCE.canonical().resolve(registryId)
                    .filter(definition -> definition.kind() == CanonicalKind.ENTITY)
                    .map(CanonicalDefinition::canonicalId)
                    .filter(value -> !value.isBlank() && !value.equals(registryId))
                    .orElse("");
            return !canonicalId.isBlank() && unlockEntry(manager, progress, player, canonicalId);
        } catch (RuntimeException | LinkageError exception) {
            disableAfterFailure(exception);
            return false;
        }
    }

    @Override
    public boolean runtimeAvailable() {
        return runtimeAvailable;
    }

    private static boolean unlockEntry(FieldGuideProgressManager manager, PlayerFieldGuideProgress progress,
                                       ServerPlayer player, String registryId) {
        ResourceLocation entry = entryId(registryId);
        if (!manager.isValidEntry(entry)) return false;
        if (progress.isUnlocked(entry)) return true;
        if (!progress.canUnlock(entry)) return false;
        progress.unlock(player, entry, "", false);
        return progress.isUnlocked(entry);
    }

    private static Set<String> entityImplementations(CanonicalDefinition definition) {
        Set<String> values = new LinkedHashSet<>();
        if (!definition.canonicalId().isBlank()) values.add(definition.canonicalId());
        values.addAll(definition.implementations());
        return values;
    }

    private void disableAfterFailure(Throwable failure) {
        if (!runtimeAvailable) return;
        runtimeAvailable = false;
        DarkFolkloreCore.LOGGER.warn("[compat/fieldguide] Exact 1.14.0 bridge disabled after {}: {}",
                failure.getClass().getSimpleName(), failure.getMessage());
    }

    private static ResourceLocation entryId(String registryId) {
        ResourceLocation id = ResourceLocation.parse(registryId);
        return ResourceLocation.fromNamespaceAndPath("entity", id.getNamespace() + "/" + id.getPath());
    }
}
