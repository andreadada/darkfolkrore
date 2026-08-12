package com.darkfolklore.core.compat.mca;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.compat.FactResult;
import com.darkfolklore.core.compat.SupernaturalStateAdapter;
import com.darkfolklore.core.knowledge.social.SecretType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Exact-version bridge for MCA Reborn x Vampirism Compat 2.0.12.
 *
 * <p>The addon exposes useful public implementation methods but no stable API package. Reflection is intentionally
 * contained here so optional class loading and future signature changes disable this adapter rather than the Core.</p>
 */
public final class McaVampCompatAdapter implements SupernaturalStateAdapter {
    public static final String MOD_ID = "mca_vamp_compat";
    public static final String TESTED_VERSION = "2.0.12";

    private Method vampireQuery;
    private Method werewolfQuery;
    private Method hunterQuery;
    private Method stateQuery;
    private Method vampireSource;
    private Method werewolfSource;
    private boolean ready;

    public void initialize() throws ReflectiveOperationException {
        Class<?> entity = Entity.class;
        Class<?> vampireService = Class.forName(
                "com.guilh.mca_vampirism_compat.service.McaVampireStateService", false,
                McaVampCompatAdapter.class.getClassLoader());
        Class<?> werewolfService = Class.forName(
                "com.guilh.mca_vampirism_compat.service.McaWerewolfStateService", false,
                McaVampCompatAdapter.class.getClassLoader());
        Class<?> hunterService = Class.forName(
                "com.guilh.mca_vampirism_compat.service.McaHunterAlignmentService", false,
                McaVampCompatAdapter.class.getClassLoader());
        Class<?> capabilities = Class.forName(
                "com.guilh.mca_vampirism_compat.capability.ModCapabilities", false,
                McaVampCompatAdapter.class.getClassLoader());
        Class<?> state = Class.forName(
                "com.guilh.mca_vampirism_compat.VampiricVillagerState", false,
                McaVampCompatAdapter.class.getClassLoader());

        vampireQuery = vampireService.getMethod("isVampire", entity);
        werewolfQuery = werewolfService.getMethod("isWerewolf", entity);
        hunterQuery = hunterService.getMethod("isMcaHunterAligned", entity);
        stateQuery = capabilities.getMethod("get", entity);
        vampireSource = state.getMethod("getSource");
        werewolfSource = state.getMethod("getWerewolfSourceUuid");
        ready = true;
    }

    @Override
    public String modId() {
        return MOD_ID;
    }

    @Override
    public FactResult isVampire(Entity entity) {
        return query(entity, vampireQuery);
    }

    @Override
    public FactResult isWerewolf(Entity entity) {
        return query(entity, werewolfQuery);
    }

    @Override
    public FactResult isHunter(Entity entity) {
        return query(entity, hunterQuery);
    }

    @Override
    public Optional<UUID> conversionSource(Entity entity, SecretType type) {
        if (!ready || !applies(entity) || (type != SecretType.VAMPIRE && type != SecretType.WEREWOLF)) {
            return Optional.empty();
        }
        try {
            Object value = stateQuery.invoke(null, entity);
            if (!(value instanceof Optional<?> state) || state.isEmpty()) return Optional.empty();
            Method source = type == SecretType.VAMPIRE ? vampireSource : werewolfSource;
            Object sourceValue = source.invoke(state.get());
            if (sourceValue instanceof Optional<?> optional && optional.orElse(null) instanceof UUID uuid) {
                return Optional.of(uuid);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            ready = false;
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp] Provenance query failed; returning unknown", exception);
        }
        return Optional.empty();
    }

    private FactResult query(Entity entity, Method method) {
        if (!applies(entity)) return FactResult.NOT_APPLICABLE;
        if (!ready || method == null) return FactResult.UNKNOWN;
        try {
            return FactResult.of((boolean) method.invoke(null, entity));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            ready = false;
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp] State query failed; returning unknown", exception);
            return FactResult.UNKNOWN;
        }
    }

    private static boolean applies(Entity entity) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && id.getNamespace().equals("mca");
    }
}
