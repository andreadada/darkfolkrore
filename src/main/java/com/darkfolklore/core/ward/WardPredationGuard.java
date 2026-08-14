package com.darkfolklore.core.ward;

import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.predation.PredatorKind;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/**
 * Repels only wild Vampirism mob target changes toward prey protected by a Dark Folklore vampire ward.
 * MCA-vampire target/navigation remains entirely provider-owned.
 */
public final class WardPredationGuard {
    public static final WardPredationGuard INSTANCE = new WardPredationGuard();
    private static final int REPEL_THRESHOLD = 60;

    private WardPredationGuard() {}

    @SubscribeEvent
    public void onTargetChange(LivingChangeTargetEvent event) {
        if (!FolkloreConfig.WARDS.get() || !FolkloreConfig.VAMPIRE_PREDATION.get()) return;
        if (!(event.getEntity() instanceof Mob predator) || !(predator.level() instanceof ServerLevel level)) return;
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null || !target.isAlive()) return;

        boolean mca = "mca".equals(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getNamespace());
        boolean animal = target instanceof Animal;
        if (!animal && !mca) return;
        if (target instanceof TamableAnimal tame && tame.isTame()) return;

        var bridge = CompatibilityManager.INSTANCE.vampirePredation();
        if (!bridge.runtimeAvailable() || bridge.predatorKind(predator) != PredatorKind.WILD_VAMPIRISM
                || !bridge.canWildFeed(predator, target)) return;

        int strength = WardEngine.INSTANCE.strengthAt(level, target.getX(), target.getY(), target.getZ(), WardType.VAMPIRE);
        if (strength >= REPEL_THRESHOLD) event.setCanceled(true);
    }
}
