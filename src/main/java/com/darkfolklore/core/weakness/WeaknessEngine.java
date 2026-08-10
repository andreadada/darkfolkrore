package com.darkfolklore.core.weakness;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.traits.TraitResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class WeaknessEngine {
    public static final WeaknessEngine INSTANCE = new WeaknessEngine();
    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);
    private WeaknessEngine() {}

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!FolkloreConfig.WEAKNESSES.get() || event.getEntity().level().isClientSide || PROCESSING.get()) return;
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || weapon.isEmpty()) return;
        var creatureTraits = TraitResolver.creatureTraits(event.getEntity());
        var itemTraits = TraitResolver.itemTraits(weapon);
        if (creatureTraits.isEmpty() || itemTraits.isEmpty()) return;
        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).getNamespace();
        try {
            PROCESSING.set(true);
            WeaknessCalculator.Result result = WeaknessCalculator.calculate(event.getAmount(), creatureTraits,
                    itemTraits, namespace, FolkloreDataManager.INSTANCE.weaknesses().rules());
            if (result.modified()) {
                event.setAmount(result.finalDamage());
                if (FolkloreConfig.DEBUG_LOGGING.get()) {
                    DarkFolkloreCore.LOGGER.debug("[weakness] {} x{} against {}", result.appliedRule(),
                            result.multiplier(), BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()));
                }
            }
        } finally {
            PROCESSING.set(false);
        }
    }
}
