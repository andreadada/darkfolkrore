package com.darkfolklore.core.encounter;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.api.event.ConfirmedLivingDeathEvent;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.investigation.EvidenceRecord;
import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.persistence.InvestigationSavedData;
import com.darkfolklore.core.persistence.WorldPosition;
import com.darkfolklore.core.society.story.PersistentStory;
import com.darkfolklore.core.society.story.StoryInstance;
import com.darkfolklore.core.society.story.StoryStatus;
import com.darkfolklore.core.society.village.VillageKey;
import com.darkfolklore.core.ward.WardEngine;
import com.darkfolklore.core.ward.WardType;
import com.darkfolklore.core.world.WorldEventDirector;
import com.darkfolklore.core.world.WorldEventType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-authoritative story manifestation director. It never registers replacement mobs and never mutates
 * MCA/Vampirism factual state. Provider entities are materializations of a persisted Dark Folklore story.
 */
public final class LegendaryEncounterEngine {
    public static final LegendaryEncounterEngine INSTANCE = new LegendaryEncounterEngine();
    private static final int EVALUATE_INTERVAL = 200;
    private static final long EVIDENCE_LIFETIME = 48000L;
    private static final String WILD_HUNT_FOLLOWER = "occultism:wild_hunt_skeleton";

    private LegendaryEncounterEngine() {}

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!FolkloreConfig.LEGENDARY_ENCOUNTERS.get()) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % EVALUATE_INTERVAL != 0) return;
        LegendaryEncounterSavedData data = LegendaryEncounterSavedData.get(server);
        long now = server.overworld().getGameTime();
        if (server.getTickCount() % 1200 == 0) data.prune(now, FolkloreConfig.HISTORY_RETENTION.get());
        seedRegionalEncounters(server, data, now);
        for (EncounterInstance encounter : new ArrayList<>(data.encounters())) advance(server, data, encounter, now);
    }

    @SubscribeEvent
    public void onConfirmedDeath(ConfirmedLivingDeathEvent event) {
        if (!FolkloreConfig.LEGENDARY_ENCOUNTERS.get()) return;
        LivingEntity victim = event.entity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        MinecraftServer server = event.server();
        LegendaryEncounterSavedData data = LegendaryEncounterSavedData.get(server);
        long now = level.getGameTime();

        data.byManifestation(victim.getUUID()).ifPresent(encounter -> {
            encounter.resolve("manifestation_killed");
            EncounterCatalog.byId(encounter.definitionId()).ifPresent(definition ->
                    data.markRegionCooldown(encounter.region(), now + definition.regionalCooldownTicks()));
            data.put(encounter);
            closeUnclaimedStory(server, encounter);
            affectResolution(server, encounter);
            notifyNearby(level, encounter.anchor().blockPos(), 96,
                    "The presence behind " + encounter.concept() + " has been broken.");
        });

        Entity killer = event.source().getEntity();
        if (victim instanceof Animal animal && suspiciousLivestockDeath(animal, killer, level)) {
            String region = VillageKey.at(level, victim.blockPosition()).serialized();
            LegendaryEncounterSavedData.LivestockPanic panic = data.recordLivestockDeath(region, now);
            if (panic.count() >= FolkloreConfig.LIVESTOCK_PANIC_THRESHOLD.get()
                    && data.regionReady(region, now) && activeCount(data) < maxActive()) {
                createEncounter(data, EncounterCatalog.CHUPACABRA, EncounterOrigin.BLOOD_EVENT,
                        level, victim.blockPosition(), now, null);
                data.clearLivestockPanic(region);
            }
        }

        boolean socialPerson = victim instanceof AbstractVillager
                || BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).getNamespace().equals("mca");
        if (socialPerson && killer instanceof LivingEntity livingKiller && livingKiller != victim) {
            String region = VillageKey.at(level, victim.blockPosition()).serialized();
            if (data.regionReady(region, now) && activeCount(data) < maxActive()
                    && deterministicChance(victim.getUUID(), Math.floorDiv(level.getDayTime(), 24000L),
                    FolkloreConfig.REVENANT_DEATH_CHANCE.get())) {
                PersonSnapshot snapshot = new PersonSnapshot(victim.getUUID(), victim.getName().getString(),
                        Optional.of(livingKiller.getUUID()), region, false);
                createEncounter(data, EncounterCatalog.REVENANT, EncounterOrigin.VIOLENT_DEATH,
                        level, victim.blockPosition(), now, snapshot);
            }
        }
    }

    private static boolean suspiciousLivestockDeath(Animal victim, Entity killer, ServerLevel level) {
        if (!level.isNight()) return false;
        if (victim instanceof TamableAnimal tame && tame.isTame()) return false;
        if (killer instanceof Player) return false;
        if (killer == null) return true;
        ResourceLocation killerId = BuiltInRegistries.ENTITY_TYPE.getKey(killer.getType());
        return "cnc:chupacabra".equals(killerId.toString());
    }

    private void seedRegionalEncounters(MinecraftServer server, LegendaryEncounterSavedData data, long now) {
        if (activeCount(data) >= maxActive()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level) || !level.isNight()) continue;
            String region = VillageKey.at(level, player.blockPosition()).serialized();
            if (!data.regionReady(region, now)) continue;
            long day = Math.floorDiv(level.getDayTime(), 24000L);

            if (WorldEventDirector.INSTANCE.isActive(level, WorldEventType.FULL_MOON)
                    && deterministicChance(region.hashCode(), day, FolkloreConfig.WILD_HUNT_FULL_MOON_CHANCE.get())) {
                if (createEncounter(data, EncounterCatalog.WILD_HUNT, EncounterOrigin.WORLD_OMEN,
                        level, player.blockPosition(), now, null).isPresent()) return;
            }

            // 0.8 uses a narrative traveller. No MCA entity is discarded, hidden, converted or replaced.
            if (deterministicChance(region.hashCode() * 31L + 17L, day, FolkloreConfig.WENDIGO_DAILY_CHANCE.get())) {
                PersonSnapshot traveller = new PersonSnapshot(UUID.randomUUID(), "Missing traveller",
                        Optional.empty(), region, false);
                if (createEncounter(data, EncounterCatalog.WENDIGO, EncounterOrigin.LOST_PERSON,
                        level, player.blockPosition(), now, traveller).isPresent()) return;
            }
        }
    }

    public Optional<EncounterInstance> createEncounter(LegendaryEncounterSavedData data, EncounterDefinition definition,
                                                       EncounterOrigin origin, ServerLevel level, BlockPos anchor,
                                                       long now, PersonSnapshot person) {
        if (!FolkloreConfig.LEGENDARY_ENCOUNTERS.get()) return Optional.empty();
        String region = VillageKey.at(level, anchor).serialized();
        if (!definition.origins().contains(origin) || !data.regionReady(region, now) || activeCount(data) >= maxActive()
                || !providerAvailable(definition)) {
            return Optional.empty();
        }
        EncounterInstance encounter = new EncounterInstance(UUID.randomUUID(), definition.id(), definition.concept(),
                definition.implementation(), definition.rank(), definition.spawnMode(), origin,
                WorldPosition.of(level, anchor), region, now,
                now + Math.max(200L, definition.omenIntervalTicks() / 2), now + definition.lifetimeTicks());
        if (person != null) encounter.setOriginPerson(person);
        encounter.transition(EncounterStage.OMENS, now + Math.max(200L, definition.omenIntervalTicks() / 2));
        if (!data.put(encounter)) return Optional.empty();
        seedOriginConsequences(level.getServer(), encounter);
        createNarrativeStory(level.getServer(), encounter, definition, now).ifPresent(encounter::bindStory);
        data.put(encounter);
        notifyNearby(level, anchor, 96, originMessage(encounter));
        DarkFolkloreCore.LOGGER.info("[encounter] created {} origin={} region={}", definition.id(), origin, region);
        return Optional.of(encounter);
    }

    private static boolean providerAvailable(EncounterDefinition definition) {
        ResourceLocation id = ResourceLocation.tryParse(definition.implementation());
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return false;
        if (!definition.id().equals(EncounterCatalog.WILD_HUNT.id())) return true;
        ResourceLocation follower = ResourceLocation.tryParse(WILD_HUNT_FOLLOWER);
        return follower != null && BuiltInRegistries.ENTITY_TYPE.containsKey(follower);
    }

    private void advance(MinecraftServer server, LegendaryEncounterSavedData data, EncounterInstance encounter, long now) {
        if (encounter.stage().terminal()) return;
        EncounterDefinition definition = EncounterCatalog.byId(encounter.definitionId()).orElse(null);
        if (definition == null) {
            expireEncounter(server, data, encounter, "missing_definition", now, 0L);
            return;
        }
        if (now >= encounter.expiresAt()) {
            expireEncounter(server, data, encounter, "lifetime_elapsed", now, definition.regionalCooldownTicks() / 2);
            return;
        }
        if (now < encounter.nextStageAt()) return;
        ServerLevel level = resolveLevel(server, encounter.anchor());
        if (level == null) return;
        switch (encounter.stage()) {
            case OMENS -> emitOmen(server, data, encounter, definition, level, now);
            case ELIGIBLE -> manifest(data, encounter, definition, level, now);
            case MANIFESTED -> {
                encounter.transition(EncounterStage.ACTIVE, now + EVALUATE_INTERVAL);
                data.put(encounter);
            }
            case ACTIVE -> tickActiveEncounter(level, encounter);
            default -> { }
        }
    }

    private void expireEncounter(MinecraftServer server, LegendaryEncounterSavedData data, EncounterInstance encounter,
                                 String reason, long now, long cooldown) {
        encounter.expire(reason);
        if (cooldown > 0L) data.markRegionCooldown(encounter.region(), now + cooldown);
        data.put(encounter);
        closeUnclaimedStory(server, encounter);
    }

    private void emitOmen(MinecraftServer server, LegendaryEncounterSavedData data, EncounterInstance encounter,
                          EncounterDefinition definition, ServerLevel level, long now) {
        List<EvidenceType> evidence = definition.omenEvidence();
        if (!evidence.isEmpty()) {
            EvidenceType type = evidence.get(Math.min(encounter.omensCompleted(), evidence.size() - 1));
            BlockPos pos = nearbyOmenPos(level, encounter.anchor().blockPos(), encounter.id(), encounter.omensCompleted());
            FolkloreSavedData.get(server).addEvidence(new EvidenceRecord(UUID.randomUUID(), type,
                    encounter.concept(), null, WorldPosition.of(level, pos), now,
                    now + Math.max(EVIDENCE_LIFETIME, FolkloreConfig.EVIDENCE_LIFETIME.get()), null));
            level.sendParticles(omenParticle(type), pos.getX() + .5, pos.getY() + .25, pos.getZ() + .5,
                    8, .35, .12, .35, .01);
        }

        int count = encounter.addOmen();
        FolkloreSavedData society = FolkloreSavedData.get(server);
        var village = society.village(encounter.region());
        village.addFear(Math.max(1, encounter.rank().weight()));
        village.addSuspicion(1);
        society.setDirty();
        notifyNearby(level, encounter.anchor().blockPos(), 96, omenMessage(encounter, count));

        if (count >= definition.minimumOmens()) {
            encounter.transition(EncounterStage.ELIGIBLE, now + 400L);
        } else {
            encounter.restoreStage(EncounterStage.OMENS, count, encounter.manifestationEntity().orElse(null),
                    encounter.originPerson().orElse(null), encounter.resolution(), now + definition.omenIntervalTicks());
        }
        data.put(encounter);
    }

    private void manifest(LegendaryEncounterSavedData data, EncounterInstance encounter,
                          EncounterDefinition definition, ServerLevel level, long now) {
        if (definition.nightOnly() && !level.isNight()) return;
        ResourceLocation id = ResourceLocation.tryParse(definition.implementation());
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            expireEncounter(level.getServer(), data, encounter, "provider_entity_missing", now,
                    definition.regionalCooldownTicks() / 2);
            return;
        }
        ServerPlayer player = nearestPlayer(level, encounter.anchor().blockPos(), definition.maximumPlayerDistance() + 64);
        if (player == null) return;
        Optional<BlockPos> spawn = EncounterSpawnFinder.aroundPlayer(level, player,
                definition.minimumPlayerDistance(), definition.maximumPlayerDistance(),
                encounter.id().getMostSignificantBits() ^ now);
        if (spawn.isEmpty()) return;
        BlockPos pos = spawn.get();

        if (FolkloreConfig.WARDS.get() && blockedByWard(level, pos, encounter)) {
            encounter.restoreStage(EncounterStage.ELIGIBLE, encounter.omensCompleted(), null,
                    encounter.originPerson().orElse(null), "ward_delayed", now + 1200L);
            data.put(encounter);
            return;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        Entity entity = type.create(level);
        if (!(entity instanceof LivingEntity living)) {
            expireEncounter(level.getServer(), data, encounter, "provider_entity_create_failed", now,
                    definition.regionalCooldownTicks() / 2);
            return;
        }
        living.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, level.random.nextFloat() * 360F, 0F);
        if (!level.noCollision(living) || !level.addFreshEntity(living)) return;

        encounter.bindManifestation(living.getUUID());
        encounter.transition(EncounterStage.MANIFESTED, now + 20L);
        if (encounter.definitionId().equals(EncounterCatalog.WILD_HUNT.id())) {
            spawnWildHuntFollowers(level, pos, encounter);
        }
        data.markRegionCooldown(encounter.region(), now + definition.regionalCooldownTicks());
        data.put(encounter);
        encounter.storyId().ifPresent(storyId -> InvestigationSavedData.get(level.getServer())
                .bindCulpritForStory(storyId, living.getUUID(), id.toString(), now));
        applyManifestationBehavior(level, living, encounter);

        L2HostilityAdapter.ApplyResult l2 = ThreatPolicyRuntime.INSTANCE.applyCuratedEncounter(level, living);
        notifyNearby(level, pos, 128, manifestationMessage(encounter));
        DarkFolkloreCore.LOGGER.info("[encounter] manifested {} as {} uuid={} region={} l2={}",
                definition.id(), id, living.getUUID(), encounter.region(), l2);
    }

    private static boolean blockedByWard(ServerLevel level, BlockPos pos, EncounterInstance encounter) {
        double x = pos.getX() + .5;
        double y = pos.getY() + .5;
        double z = pos.getZ() + .5;
        if (encounter.definitionId().equals(EncounterCatalog.REVENANT.id())
                || encounter.definitionId().equals(EncounterCatalog.WILD_HUNT.id())) {
            return WardEngine.INSTANCE.blocksManifestation(level, x, y, z, WardType.SPIRIT, 60)
                    || WardEngine.INSTANCE.blocksManifestation(level, x, y, z, WardType.UNDEAD, 60);
        }
        return WardEngine.INSTANCE.blocksManifestation(level, x, y, z, WardType.GENERAL, 60);
    }

    private void spawnWildHuntFollowers(ServerLevel level, BlockPos center, EncounterInstance encounter) {
        ResourceLocation followerId = ResourceLocation.tryParse(WILD_HUNT_FOLLOWER);
        if (followerId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(followerId)) return;
        EntityType<?> followerType = BuiltInRegistries.ENTITY_TYPE.get(followerId);
        int[][] offsets = {{2, 2}, {-2, 2}, {3, -2}, {-3, -2}};
        for (int[] offset : offsets) {
            if (encounter.participants().size() >= EncounterInstance.MAX_PARTICIPANTS) break;
            BlockPos pos = center.offset(offset[0], 0, offset[1]);
            if (!level.hasChunkAt(pos)) continue;
            Entity entity = followerType.create(level);
            if (!(entity instanceof Mob follower)) continue;
            follower.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, level.random.nextFloat() * 360F, 0F);
            if (!level.noCollision(follower) || !level.addFreshEntity(follower)) continue;
            encounter.addParticipant(follower.getUUID());
            ThreatPolicyRuntime.INSTANCE.applyCuratedEncounter(level, follower);
        }
    }

    private void tickActiveEncounter(ServerLevel level, EncounterInstance encounter) {
        if (!encounter.definitionId().equals(EncounterCatalog.WILD_HUNT.id())) return;
        long seed = encounter.id().getMostSignificantBits() ^ encounter.id().getLeastSignificantBits();
        double angle = ((seed >>> 11) * 0x1.0p-53) * Math.PI * 2.0D;
        double targetX = encounter.anchor().x() + Math.cos(angle) * 96.0D;
        double targetZ = encounter.anchor().z() + Math.sin(angle) * 96.0D;
        for (UUID participant : encounter.participants()) {
            Entity entity = level.getEntity(participant);
            if (!(entity instanceof Mob mob) || !mob.isAlive() || mob.getTarget() != null) continue;
            mob.getNavigation().moveTo(targetX, mob.getY(), targetZ, 1.05D);
        }
    }

    private void applyManifestationBehavior(ServerLevel level, LivingEntity living, EncounterInstance encounter) {
        if (encounter.definitionId().equals(EncounterCatalog.REVENANT.id()) && living instanceof Mob mob) {
            encounter.originPerson().flatMap(PersonSnapshot::killer).ifPresent(killerId -> {
                Entity killer = level.getEntity(killerId);
                if (killer instanceof LivingEntity target && target.isAlive()
                        && target.distanceToSqr(living) <= 64 * 64) mob.setTarget(target);
            });
        }
    }

    private Optional<UUID> createNarrativeStory(MinecraftServer server, EncounterInstance encounter,
                                                EncounterDefinition definition, long now) {
        if (!FolkloreConfig.DYNAMIC_STORIES.get()) return Optional.empty();
        String template = switch (definition.id()) {
            case "darkfolklore:wendigo_hunger" -> "darkfolklore:missing_traveller";
            case "darkfolklore:livestock_panic" -> "darkfolklore:livestock_panic";
            case "darkfolklore:returned_dead" -> "darkfolklore:returned_dead";
            case "darkfolklore:wild_hunt" -> "darkfolklore:wild_hunt_omen";
            default -> "darkfolklore:hunter_investigation";
        };
        StoryInstance story = new StoryInstance(UUID.randomUUID(), template, encounter.concept(), now,
                now + Math.max(definition.lifetimeTicks(), FolkloreConfig.CONTRACT_LIFETIME.get()));
        encounter.originPerson().ifPresent(person -> story.addActor(person.personId()));
        FolkloreSavedData.get(server).putStory(new PersistentStory(story, encounter.anchor(), encounter.region()));
        return Optional.of(story.id());
    }

    private static void closeUnclaimedStory(MinecraftServer server, EncounterInstance encounter) {
        FolkloreSavedData society = FolkloreSavedData.get(server);
        encounter.storyId().flatMap(society::story).ifPresent(story -> {
            if (story.story().status() == StoryStatus.INCIDENT && story.story().advance(StoryStatus.EXPIRED)) {
                society.putStory(story);
            }
        });
    }

    private void seedOriginConsequences(MinecraftServer server, EncounterInstance encounter) {
        FolkloreSavedData society = FolkloreSavedData.get(server);
        var village = society.village(encounter.region());
        village.addFear(encounter.rank().weight() * 2);
        village.addSuspicion(encounter.rank().weight());
        society.setDirty();
    }

    private void affectResolution(MinecraftServer server, EncounterInstance encounter) {
        FolkloreSavedData society = FolkloreSavedData.get(server);
        var village = society.village(encounter.region());
        village.addFear(-Math.max(1, encounter.rank().weight()));
        village.addAwareness(encounter.rank().weight());
        society.setDirty();
    }

    private static String originMessage(EncounterInstance encounter) {
        if (encounter.definitionId().equals(EncounterCatalog.WENDIGO.id())) {
            return "A traveller has failed to return. The surrounding wilds feel wrong.";
        }
        if (encounter.definitionId().equals(EncounterCatalog.CHUPACABRA.id())) {
            return "Another animal has been found dead. The pattern no longer feels ordinary.";
        }
        if (encounter.definitionId().equals(EncounterCatalog.REVENANT.id())) {
            return encounter.originPerson().map(person -> "The death of " + person.displayName() + " has left something unsettled.")
                    .orElse("A recent death has left something unsettled.");
        }
        return "Stories of a spectral hunt begin to spread under the full moon.";
    }

    private static String omenMessage(EncounterInstance encounter, int omen) {
        if (encounter.definitionId().equals(EncounterCatalog.WENDIGO.id())) {
            return omen == 1 ? "Unfamiliar tracks disturb the wilderness."
                    : omen == 2 ? "Scattered remains suggest desperate hunger."
                    : "Blood and tracks now point toward something no ordinary animal explains.";
        }
        if (encounter.definitionId().equals(EncounterCatalog.CHUPACABRA.id())) {
            return omen == 1 ? "Blood is found where the livestock was taken."
                    : "A strange scent and narrow tracks deepen the livestock panic.";
        }
        if (encounter.definitionId().equals(EncounterCatalog.REVENANT.id())) {
            return omen == 1 ? "A cold spiritual echo lingers near the place of death."
                    : "The dead do not seem entirely absent from this place.";
        }
        return omen == 1 ? "An occult presence gathers beneath the moon."
                : "Many spectral tracks now seem to move as one hunt.";
    }

    private static String manifestationMessage(EncounterInstance encounter) {
        if (encounter.definitionId().equals(EncounterCatalog.WENDIGO.id())) return "The hunger in the wilderness has taken form.";
        if (encounter.definitionId().equals(EncounterCatalog.CHUPACABRA.id())) return "The predator behind the livestock deaths is near.";
        if (encounter.definitionId().equals(EncounterCatalog.REVENANT.id())) return "Something returned from death now walks nearby.";
        return "The Wild Hunt is passing through this region.";
    }

    private static void notifyNearby(ServerLevel level, BlockPos center, double radius, String message) {
        double radiusSq = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center.getX() + .5, center.getY() + .5, center.getZ() + .5) <= radiusSq) {
                player.displayClientMessage(Component.literal(message), true);
            }
        }
    }

    private static ServerLevel resolveLevel(MinecraftServer server, WorldPosition pos) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(pos.dimension())) return level;
        }
        return null;
    }

    private static ServerPlayer nearestPlayer(ServerLevel level, BlockPos pos, double maxDistance) {
        ServerPlayer best = null;
        double bestDist = maxDistance * maxDistance;
        for (ServerPlayer player : level.players()) {
            double dist = player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
            if (dist < bestDist) {
                best = player;
                bestDist = dist;
            }
        }
        return best;
    }

    private static BlockPos nearbyOmenPos(ServerLevel level, BlockPos anchor, UUID id, int index) {
        long seed = id.getLeastSignificantBits() ^ (index * 0x9E3779B97F4A7C15L);
        int dx = (int) Math.floorMod(seed, 25) - 12;
        int dz = (int) Math.floorMod(seed >>> 16, 25) - 12;
        BlockPos raw = anchor.offset(dx, 0, dz);
        return level.hasChunkAt(raw) ? raw : anchor;
    }

    private static net.minecraft.core.particles.ParticleOptions omenParticle(EvidenceType type) {
        return switch (type) {
            case BLOOD, BITE_MARK -> net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR;
            case SPIRIT_ECHO, SOUL_ECHO, OCCULT_SIGNATURE -> net.minecraft.core.particles.ParticleTypes.SOUL;
            case FOOTPRINT, SCENT, BONE -> net.minecraft.core.particles.ParticleTypes.ASH;
            default -> net.minecraft.core.particles.ParticleTypes.SMOKE;
        };
    }

    private static int maxActive() { return FolkloreConfig.LEGENDARY_MAX_ACTIVE.get(); }

    private static int activeCount(LegendaryEncounterSavedData data) {
        return (int) data.encounters().stream().filter(value -> !value.stage().terminal()).count();
    }

    private static boolean deterministicChance(UUID id, long epoch, double chance) {
        return deterministicChance(id.getMostSignificantBits() ^ id.getLeastSignificantBits(), epoch, chance);
    }

    private static boolean deterministicChance(long id, long epoch, double chance) {
        long x = id ^ (epoch * 0x9E3779B97F4A7C15L);
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return (x >>> 11) * 0x1.0p-53 < chance;
    }
}
