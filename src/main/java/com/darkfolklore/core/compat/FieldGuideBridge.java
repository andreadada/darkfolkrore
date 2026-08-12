package com.darkfolklore.core.compat;

import net.minecraft.server.level.ServerPlayer;

/** Optional, exact-version Field Guide bridge exposed without leaking provider classes into core gameplay code. */
public interface FieldGuideBridge {
    boolean unlockObservedImplementation(ServerPlayer player, String registryId);
    boolean runtimeAvailable();
}
