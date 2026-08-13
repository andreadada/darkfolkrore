package com.darkfolklore.core.encounter;

import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.UUID;

public final class ThreatPolicyRuntime {
    public static final ThreatPolicyRuntime INSTANCE = new ThreatPolicyRuntime();
    private static final ResourceLocation VITALITY = ResourceLocation.fromNamespaceAndPath("darkfolklore", "encounter_vitality");
    private static final ResourceLocation TANK = ResourceLocation.fromNamespaceAndPath("darkfolklore", "encounter_tank");
    private static final ResourceLocation BRUTAL = ResourceLocation.fromNamespaceAndPath("darkfolklore", "encounter_brutal");
    private static final ResourceLocation SWIFT = ResourceLocation.fromNamespaceAndPath("darkfolklore", "encounter_swift");
    private static final ResourceLocation GENERIC_VITALITY = ResourceLocation.fromNamespaceAndPath("darkfolklore", "hostile_vitality");
    private static final ResourceLocation GENERIC_BRUTAL = ResourceLocation.fromNamespaceAndPath("darkfolklore", "hostile_brutal");
    private static final int MAX_PENDING_L2 = 4096;
    private final LinkedHashMap<UUID, PendingL2> pendingL2 = new LinkedHashMap<>();

    private ThreatPolicyRuntime() {}

    public void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (!FolkloreConfig.SPAWN_DIRECTOR.get() || event.getSpawnType() != MobSpawnType.NATURAL) return;
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
        EncounterPolicy policy = ThreatPolicyManager.INSTANCE.forEntity(entityId).orElse(null);
        double multiplier;
        if (policy != null) multiplier = policy.naturalSpawnMultiplier();
        else if (event.getEntity() instanceof Monster) multiplier = FolkloreConfig.HOSTILE_NATURAL_SPAWN_MULTIPLIER.get();
        else return;
        if (multiplier < 1.0D && event.getLevel().getRandom().nextDouble() > multiplier) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!FolkloreConfig.ENCOUNTER_DIRECTOR.get() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof LivingEntity living)) return;
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();
        EncounterPolicy policy = ThreatPolicyManager.INSTANCE.forEntity(entityId).orElse(null);
        if (policy == null) {
            if (!(living instanceof Monster)) return;
            boolean refreshHealth = applyMultiplier(living, Attributes.MAX_HEALTH, GENERIC_VITALITY,
                    FolkloreConfig.HOSTILE_HEALTH_MULTIPLIER.get() - 1.0D);
            applyMultiplier(living, Attributes.ATTACK_DAMAGE, GENERIC_BRUTAL,
                    FolkloreConfig.HOSTILE_DAMAGE_MULTIPLIER.get() - 1.0D);
            if (refreshHealth) living.setHealth(living.getMaxHealth());
            requestL2(level, living, FolkloreConfig.L2_GENERIC_MIN_LEVEL.get());
            return;
        }
        boolean refreshHealth = applyMultiplier(living, Attributes.MAX_HEALTH, VITALITY, policy.vitalityMultiplier() - 1.0D);
        if (policy.guaranteedTraits().contains(ThreatTrait.TANK)) refreshHealth |= applyMultiplier(living, Attributes.MAX_HEALTH, TANK, 0.25D);
        if (policy.guaranteedTraits().contains(ThreatTrait.BRUTAL)) applyMultiplier(living, Attributes.ATTACK_DAMAGE, BRUTAL, 0.20D);
        if (policy.guaranteedTraits().contains(ThreatTrait.SWIFT)) applyMultiplier(living, Attributes.MOVEMENT_SPEED, SWIFT, 0.12D);
        if (refreshHealth) living.setHealth(living.getMaxHealth());
        var nearest = level.players().stream().filter(player -> player.distanceToSqr(living) < 16384.0D)
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(living))).orElse(null);
        if (nearest != null && policy.minimumEncounterPressure() > 0) {
            FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
            if (data.encounterPressure(nearest.getUUID()) < policy.minimumEncounterPressure()) {
                data.setEncounterPressure(nearest.getUUID(), policy.minimumEncounterPressure());
            }
        }
        requestL2(level, living, policy.l2MinimumLevel());
    }

    public void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !(living.level() instanceof ServerLevel level)) return;
        PendingL2 pending = pendingL2.get(living.getUUID());
        if (pending == null) return;
        long now = level.getGameTime();
        if (now > pending.expiresAt()) {
            pendingL2.remove(living.getUUID());
            return;
        }
        if (living.tickCount % 10 != 0) return;
        L2HostilityAdapter.ApplyResult result = L2HostilityAdapter.INSTANCE.applyMinimum(living, pending.minimumLevel());
        if (!result.retry()) pendingL2.remove(living.getUUID());
    }

    public void clearRuntimeState() {
        pendingL2.clear();
        L2HostilityAdapter.INSTANCE.clearRuntimeState();
    }

    private void requestL2(ServerLevel level, LivingEntity living, int minimumLevel) {
        if (minimumLevel <= 0) return;
        L2HostilityAdapter.ApplyResult result = L2HostilityAdapter.INSTANCE.applyMinimum(living, minimumLevel);
        if (!result.retry()) return;
        if (pendingL2.size() >= MAX_PENDING_L2) pendingL2.remove(pendingL2.keySet().iterator().next());
        pendingL2.put(living.getUUID(), new PendingL2(minimumLevel, level.getGameTime() + 200L));
    }

    private static boolean applyMultiplier(LivingEntity living, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        if (amount <= 0.0D) return false;
        var instance = living.getAttribute(attribute);
        if (instance == null) return false;
        boolean fresh = instance.getModifier(id) == null;
        instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return fresh;
    }

    private record PendingL2(int minimumLevel, long expiresAt) {}
}
