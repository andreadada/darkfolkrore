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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import java.util.Comparator;

public final class ThreatPolicyRuntime {
    public static final ThreatPolicyRuntime INSTANCE = new ThreatPolicyRuntime();
    private static final ResourceLocation VITALITY = ResourceLocation.fromNamespaceAndPath("darkfolklore", "encounter_vitality");
    private static final ResourceLocation TANK = ResourceLocation.fromNamespaceAndPath("darkfolklore", "encounter_tank");
    private static final ResourceLocation BRUTAL = ResourceLocation.fromNamespaceAndPath("darkfolklore", "encounter_brutal");
    private static final ResourceLocation SWIFT = ResourceLocation.fromNamespaceAndPath("darkfolklore", "encounter_swift");

    private ThreatPolicyRuntime() {}

    @SubscribeEvent
    public void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (!FolkloreConfig.SPAWN_DIRECTOR.get() || event.getSpawnType() != MobSpawnType.NATURAL) return;
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
        EncounterPolicy policy = ThreatPolicyManager.INSTANCE.forEntity(entityId).orElse(null);
        if (policy == null || policy.naturalSpawnMultiplier() >= 1.0D) return;
        if (event.getLevel().getRandom().nextDouble() > policy.naturalSpawnMultiplier()) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!FolkloreConfig.ENCOUNTER_DIRECTOR.get() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof LivingEntity living)) return;
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();
        EncounterPolicy policy = ThreatPolicyManager.INSTANCE.forEntity(entityId).orElse(null);
        if (policy == null) return;

        boolean refreshHealth = applyMultiplier(living, Attributes.MAX_HEALTH, VITALITY, policy.vitalityMultiplier() - 1.0D);
        if (policy.guaranteedTraits().contains(ThreatTrait.TANK)) {
            refreshHealth |= applyMultiplier(living, Attributes.MAX_HEALTH, TANK, 0.25D);
        }
        if (policy.guaranteedTraits().contains(ThreatTrait.BRUTAL)) {
            applyMultiplier(living, Attributes.ATTACK_DAMAGE, BRUTAL, 0.20D);
        }
        if (policy.guaranteedTraits().contains(ThreatTrait.SWIFT)) {
            applyMultiplier(living, Attributes.MOVEMENT_SPEED, SWIFT, 0.12D);
        }
        if (refreshHealth) living.setHealth(living.getMaxHealth());

        var nearest = level.players().stream()
                .filter(player -> player.distanceToSqr(living) < 16384.0D)
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(living))).orElse(null);
        if (nearest != null && policy.minimumEncounterPressure() > 0) {
            FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
            if (data.encounterPressure(nearest.getUUID()) < policy.minimumEncounterPressure()) {
                data.setEncounterPressure(nearest.getUUID(), policy.minimumEncounterPressure());
            }
        }

        L2HostilityAdapter.INSTANCE.applyMinimum(living, policy.l2MinimumLevel());
    }

    private static boolean applyMultiplier(LivingEntity living, Holder<Attribute> attribute,
                                           ResourceLocation id, double amount) {
        if (amount <= 0.0D) return false;
        var instance = living.getAttribute(attribute);
        if (instance == null) return false;
        boolean fresh = instance.getModifier(id) == null;
        instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return fresh;
    }
}
