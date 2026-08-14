package com.darkfolklore.core.society.village;

import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.knowledge.social.SecretType;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.society.story.SocietyStoryEngine;
import com.darkfolklore.core.society.story.StoryTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Makes persisted society pressure visible without spawning fake provider factions or mutating MCA/Vampirism AI.
 * Announcements are edge-triggered and bounded per online player. Major observed village-state transitions are
 * delegated to the existing persistent SocietyStoryEngine rather than creating a second narrative store.
 */
public final class VillageResponseEngine {
    public static final VillageResponseEngine INSTANCE = new VillageResponseEngine();
    private static final int MAX_PLAYER_OBSERVATIONS = 512;
    private static final int MAX_VILLAGE_TIERS = 1024;
    private final LinkedHashMap<UUID, Observation> observations = new LinkedHashMap<>();
    private final LinkedHashMap<String, VillageResponseTier> villageTiers = new LinkedHashMap<>();

    private VillageResponseEngine() {}

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!FolkloreConfig.VILLAGE_SOCIETY.get() || !(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % 200 != 0) return;
        String village = VillageKey.at(player.serverLevel(), player.blockPosition()).serialized();
        VillageSocietyState state = FolkloreSavedData.get(player.getServer()).village(village);
        VillageResponseRules.Snapshot snapshot = VillageResponseRules.assess(state);

        VillageResponseTier previousVillageTier = villageTiers.remove(village);
        villageTiers.put(village, snapshot.tier());
        trim(villageTiers, MAX_VILLAGE_TIERS);
        if (previousVillageTier != null && previousVillageTier != snapshot.tier()) {
            persistMajorTransition(player, previousVillageTier, snapshot.tier());
        }

        Observation previous = observations.remove(player.getUUID());
        observations.put(player.getUUID(), new Observation(village, snapshot));
        trim(observations, MAX_PLAYER_OBSERVATIONS);
        boolean changedVillage = previous == null || !previous.village().equals(village);
        boolean changedTier = previous == null || previous.snapshot().tier() != snapshot.tier();
        if ((changedVillage || changedTier) && snapshot.tier() != VillageResponseTier.CALM) {
            player.displayClientMessage(Component.literal("Village response: " + snapshot.tier()
                    + " — " + snapshot.message()), false);
        }
    }

    private static void persistMajorTransition(ServerPlayer observer, VillageResponseTier from,
                                               VillageResponseTier to) {
        StoryTrigger trigger;
        String concept;
        Optional<SecretType> secret;
        if (to == VillageResponseTier.COMPROMISED) {
            trigger = StoryTrigger.VILLAGE_COMPROMISED;
            concept = "darkfolklore:vampire";
            secret = Optional.of(SecretType.VAMPIRE);
        } else if ((to == VillageResponseTier.MOBILIZED || to == VillageResponseTier.LOCKDOWN)
                && from.ordinal() < VillageResponseTier.MOBILIZED.ordinal()) {
            trigger = StoryTrigger.VILLAGE_MOBILIZATION;
            concept = "darkfolklore:hunter";
            secret = Optional.of(SecretType.HUNTER);
        } else {
            return;
        }
        SocietyStoryEngine.INSTANCE.maybeCreate(observer.serverLevel(), observer.blockPosition(), trigger,
                concept, secret, List.of(), false, 5.0D);
    }

    private static <K, V> void trim(LinkedHashMap<K, V> values, int max) {
        while (values.size() > max) values.remove(values.keySet().iterator().next());
    }

    public Optional<Observation> observation(UUID player) { return Optional.ofNullable(observations.get(player)); }
    public int trackedVillages() { return villageTiers.size(); }

    public void clearRuntimeState() {
        observations.clear();
        villageTiers.clear();
    }

    public record Observation(String village, VillageResponseRules.Snapshot snapshot) {}
}
