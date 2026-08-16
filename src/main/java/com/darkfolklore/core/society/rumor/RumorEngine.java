package com.darkfolklore.core.society.rumor;

import com.darkfolklore.core.api.event.RumorSpreadEvent;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.knowledge.social.*;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.society.SocialEntityClassifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/** Bounded, local rumor transmission. It never recursively scans society. */
public final class RumorEngine {
    public static final RumorEngine INSTANCE = new RumorEngine();
    private static final int MAX_QUEUE = 1024;
    private static final int MAX_DIAGNOSTICS = 128;
    private static final int MAX_LOCAL_CANDIDATES = 24;
    private static final int MAX_COOLDOWNS = 4096;
    private final ArrayDeque<RumorTask> queue = new ArrayDeque<>();
    private final LinkedHashMap<RumorCooldownKey, Long> cooldowns = new LinkedHashMap<>();
    private final ArrayDeque<RumorDiagnostic> diagnostics = new ArrayDeque<>();

    private RumorEngine() {}

    public void enqueue(LivingEntity sender, UUID subject, SecretType secret,
                        SocialKnowledgeRecord knowledge, int hops) {
        if (!FolkloreConfig.RUMORS.get() || queue.size() >= MAX_QUEUE || hops > 3) return;
        if (sender.level() instanceof ServerLevel level
                && FolkloreSavedData.get(level.getServer()).rumorsSilenced(sender.getUUID(), level.getGameTime())) {
            return;
        }
        queue.addLast(new RumorTask(sender.getUUID(), subject, secret, knowledge,
                sender.level().dimension().location().toString(), hops));
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        if (FolkloreConfig.RUMORS.get() && now % FolkloreConfig.RUMOR_INTERVAL.get() == 0) {
            int budget = FolkloreConfig.RUMORS_PER_BATCH.get();
            while (budget-- > 0 && !queue.isEmpty()) process(server, queue.removeFirst(), now);
        }
        if (now % 1200 == 0) {
            FolkloreSavedData data = FolkloreSavedData.get(server);
            data.decayRumors(now, FolkloreConfig.RUMOR_HALF_LIFE.get(), 0.08F);
            data.pruneSocial(now, 0.12F, FolkloreConfig.RUMOR_HALF_LIFE.get() * 4L);
            data.pruneEvidence(now);
            data.pruneNarrativeHistory(now, FolkloreConfig.HISTORY_RETENTION.get());
            data.pruneRumorSilence(now);
            cooldowns.entrySet().removeIf(entry -> now - entry.getValue() > FolkloreConfig.RUMOR_HALF_LIFE.get());
            trimOldest(cooldowns, MAX_COOLDOWNS);
        }
    }

    private void process(MinecraftServer server, RumorTask task, long now) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(task.dimension());
        if (dimensionId == null) return;
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (level == null || !(level.getEntity(task.sender()) instanceof LivingEntity sender) || !sender.isAlive()) return;
        RumorCooldownKey cooldownKey = new RumorCooldownKey(task.sender(), task.subject(), task.secret());
        long last = cooldowns.getOrDefault(cooldownKey, Long.MIN_VALUE / 2);
        if (now - last < FolkloreConfig.RUMOR_INTERVAL.get() * 4L) return;

        FolkloreSavedData data = FolkloreSavedData.get(server);
        if (data.rumorsSilenced(sender.getUUID(), now)) return;
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                sender.getBoundingBox().inflate(12.0D), recipient -> recipient.isAlive()
                        && SocialEntityClassifier.isSocial(recipient)
                        && !recipient.getUUID().equals(sender.getUUID())
                        && !recipient.getUUID().equals(task.subject())
                        && (!(recipient instanceof Player player) || !player.isSpectator()));
        Collections.shuffle(candidates, new Random(level.getRandom().nextLong()));
        if (candidates.size() > MAX_LOCAL_CANDIDATES) {
            candidates = candidates.subList(0, MAX_LOCAL_CANDIDATES);
        }
        for (LivingEntity recipient : candidates) {
            SocialKnowledgeKey key = new SocialKnowledgeKey(recipient.getUUID(), task.subject(), task.secret());
            if (data.social(key).map(value -> value.state().strength() >= SocialKnowledgeState.CONFIRMED.strength())
                    .orElse(false)) continue;
            SocialTrustAssessment trust = SocialTrustResolver.evaluate(data, level, recipient, sender,
                    task.subject(), task.secret());
            double chance = Math.min(1.0D, FolkloreConfig.RUMOR_CHANCE.get() * trust.transmissionMultiplier());
            if (level.getRandom().nextDouble() > chance) {
                recordDiagnostic(new RumorDiagnostic(now, sender.getUUID(), recipient.getUUID(), task.subject(),
                        task.secret(), trust.trust(), task.knowledge().confidence(), 0.0F, false,
                        "transmission roll rejected", trust.contributions()));
                return;
            }
            SocialKnowledgeRecord retold = RumorRules.retell(task.knowledge(), trust.trust(), now);
            if (retold.confidence() < 0.1F) {
                recordDiagnostic(new RumorDiagnostic(now, sender.getUUID(), recipient.getUUID(), task.subject(),
                        task.secret(), trust.trust(), task.knowledge().confidence(), retold.confidence(), false,
                        "confidence below delivery floor", trust.contributions()));
                return;
            }
            SocialKnowledgeRecord merged = data.mergeSocial(key, retold);
            cooldowns.remove(cooldownKey);
            cooldowns.put(cooldownKey, now);
            trimOldest(cooldowns, MAX_COOLDOWNS);
            recordDiagnostic(new RumorDiagnostic(now, sender.getUUID(), recipient.getUUID(), task.subject(),
                    task.secret(), trust.trust(), task.knowledge().confidence(), merged.confidence(), true,
                    "delivered", trust.contributions()));
            NeoForge.EVENT_BUS.post(new RumorSpreadEvent(sender.getUUID(), recipient.getUUID(),
                    task.subject(), task.secret(), merged));
            if (task.hops() < 3 && merged.confidence() >= 0.2F) {
                enqueue(recipient, task.subject(), task.secret(), merged, task.hops() + 1);
            }
            return;
        }
    }

    private void recordDiagnostic(RumorDiagnostic diagnostic) {
        diagnostics.addLast(diagnostic);
        while (diagnostics.size() > MAX_DIAGNOSTICS) diagnostics.removeFirst();
    }

    public int queued() { return queue.size(); }
    public List<RumorDiagnostic> diagnostics() { return List.copyOf(diagnostics); }

    /** Clears non-persistent queue/cooldown/diagnostic state when a server lifecycle ends. */
    public void clearRuntimeState() {
        queue.clear();
        cooldowns.clear();
        diagnostics.clear();
    }

    private static <K, V> void trimOldest(LinkedHashMap<K, V> map, int maximum) {
        while (map.size() > maximum) {
            Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
            if (!iterator.hasNext()) return;
            iterator.next();
            iterator.remove();
        }
    }

    private record RumorTask(UUID sender, UUID subject, SecretType secret,
                             SocialKnowledgeRecord knowledge, String dimension, int hops) {}
    private record RumorCooldownKey(UUID sender, UUID subject, SecretType secret) {}
}
