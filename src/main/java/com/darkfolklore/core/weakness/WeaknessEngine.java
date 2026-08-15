package com.darkfolklore.core.weakness;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.traits.ItemTrait;
import com.darkfolklore.core.traits.TraitResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.EnumSet;

public final class WeaknessEngine {
    public static final WeaknessEngine INSTANCE = new WeaknessEngine();
    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);
    private static final TagKey<DamageType> SILVER_PROJECTILE_DAMAGE = TagKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("darkfolklore", "silver_projectile"));

    private WeaknessEngine() {}

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!FolkloreConfig.WEAKNESSES.get() || event.getEntity().level().isClientSide || PROCESSING.get()) return;

        var creatureTraits = TraitResolver.creatureTraits(event.getEntity());
        if (creatureTraits.isEmpty()) return;

        EnumSet<ItemTrait> sourceTraits = EnumSet.noneOf(ItemTrait.class);
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon != null && !weapon.isEmpty()) sourceTraits.addAll(TraitResolver.itemTraits(weapon));

        // Projectile damage often retains the launcher rather than the consumed ammunition as DamageSource weapon.
        // Damage-type tags let exact pack integrations preserve ammunition semantics without linking provider classes.
        if (event.getSource().is(SILVER_PROJECTILE_DAMAGE)) sourceTraits.add(ItemTrait.SILVER_PROJECTILE);
        if (sourceTraits.isEmpty()) return;

        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).getNamespace();
        try {
            PROCESSING.set(true);
            WeaknessCalculator.Result result = WeaknessCalculator.calculate(event.getAmount(), creatureTraits,
                    sourceTraits, namespace, FolkloreDataManager.INSTANCE.weaknesses().rules());
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
