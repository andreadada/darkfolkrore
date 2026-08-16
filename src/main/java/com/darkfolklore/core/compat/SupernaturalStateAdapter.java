package com.darkfolklore.core.compat;

import com.darkfolklore.core.knowledge.social.SecretType;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.UUID;

public interface SupernaturalStateAdapter {
    String modId();
    FactResult isVampire(Entity entity);
    FactResult isWerewolf(Entity entity);
    FactResult isHunter(Entity entity);

    default Optional<UUID> conversionSource(Entity entity, SecretType type) {
        return Optional.empty();
    }

    /** Clears server-lifecycle-local failures/caches without resolving or registering the adapter a second time. */
    default void clearRuntimeState() {}
}
