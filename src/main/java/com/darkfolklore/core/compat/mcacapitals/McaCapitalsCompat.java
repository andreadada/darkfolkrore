package com.darkfolklore.core.compat.mcacapitals;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.compat.CompatCapabilityCircuit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Exact-version, read-only MCA Capitals 1.1.0 bridge.
 * Political role resolution and direct MCA-village lookup are independent capabilities.
 */
public final class McaCapitalsCompat {
    public static final String MOD_ID = "mcacapitals";
    public static final String TESTED_VERSION = "1.1.0";
    public static final int ROLE_CACHE_MAX_ENTRIES = 1024;
    public static final long ROLE_CACHE_TTL_TICKS = 20;

    private final CompatCapabilityCircuit identityReads = new CompatCapabilityCircuit("capital-identity");
    private final CompatCapabilityCircuit roleReads = new CompatCapabilityCircuit("political-role");
    private final CompatCapabilityCircuit villageLookup = new CompatCapabilityCircuit("village-lookup");
    private final AtomicBoolean queryFailureLogged = new AtomicBoolean();
    private final Map<RoleCacheKey, CacheEntry> roleCache = new LinkedHashMap<>(128, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<RoleCacheKey, CacheEntry> eldest) {
            return size() > ROLE_CACHE_MAX_ENTRIES;
        }
    };

    private Class<?> capitalRecordClass;
    private Method findCapitalForEntity;
    private Method getCapitalByVillageId;
    private Method getDisplayTitle;
    private Method getCapitalId;
    private Method getVillageId;
    private Method getState;
    private Method isRoyalGuard;
    private volatile boolean ready;
    private volatile String statusDetail = "not initialized";

    public synchronized boolean initialize(String actualVersion) {
        reset();
        if (!TESTED_VERSION.equals(actualVersion)) {
            failAll("requires exact MCA Capitals " + TESTED_VERSION + ", found " + actualVersion);
            statusDetail = "disabled: " + diagnosticDetail();
            return false;
        }
        ClassLoader loader = McaCapitalsCompat.class.getClassLoader();
        try {
            capitalRecordClass = Class.forName("com.majesttyx.mcacapitals.capital.CapitalRecord", false, loader);
            getCapitalId = capitalRecordClass.getMethod("getCapitalId");
            getVillageId = capitalRecordClass.getMethod("getVillageId");
            getState = capitalRecordClass.getMethod("getState");
            identityReads.markReady("capital identity members resolved");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            failAll("capital identity signature mismatch: " + exception.getClass().getSimpleName());
            statusDetail = "disabled: " + diagnosticDetail();
            return false;
        }
        try {
            Class<?> titleResolver = Class.forName("com.majesttyx.mcacapitals.capital.CapitalTitleResolver", false, loader);
            findCapitalForEntity = titleResolver.getMethod("findCapitalForEntity", ServerLevel.class, UUID.class);
            getDisplayTitle = titleResolver.getMethod("getDisplayTitle", ServerLevel.class, capitalRecordClass, UUID.class);
            isRoyalGuard = capitalRecordClass.getMethod("isRoyalGuard", UUID.class);
            roleReads.markReady("political title/guard members resolved");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            roleReads.fail(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mcacapitals] Political role probe unavailable: {}",
                    exception.getClass().getSimpleName());
        }
        try {
            Class<?> manager = Class.forName("com.majesttyx.mcacapitals.capital.CapitalManager", false, loader);
            getCapitalByVillageId = manager.getMethod("getCapitalByVillageId", Integer.class);
            villageLookup.markReady("direct MCA village lookup resolved");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            villageLookup.fail(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mcacapitals] MCA village-id lookup unavailable; role reads remain independent: {}",
                    exception.getClass().getSimpleName());
        }
        ready = identityReads.available() && roleReads.available();
        statusDetail = (ready && villageLookup.available() ? "active: " : ready ? "partial: " : "disabled: ")
                + diagnosticDetail();
        return ready;
    }

    public boolean isReady() { return ready; }
    public boolean villageLookupAvailable() { return ready && villageLookup.available() && getCapitalByVillageId != null; }
    public String statusDetail() { return statusDetail; }
    public String diagnosticDetail() {
        return "exact MCA Capitals " + TESTED_VERSION + "; " + identityReads.detail() + ", "
                + roleReads.detail() + ", " + villageLookup.detail();
    }

    public PoliticalContext politicalContext(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return PoliticalContext.queryFailed("entity is not on a logical server");
        return politicalContext(level, entity.getUUID());
    }

    public PoliticalContext politicalContext(ServerLevel level, UUID entityId) {
        if (!ready) return PoliticalContext.disabled(statusDetail);
        long now = level.getGameTime();
        RoleCacheKey key = new RoleCacheKey(System.identityHashCode(level.getServer()), level.dimension().location().toString(), entityId);
        synchronized (roleCache) {
            CacheEntry cached = roleCache.get(key);
            if (cached != null && now >= cached.createdAt && now < cached.expiresAt) return cached.context;
        }
        PoliticalContext context = queryPoliticalContext(level, entityId);
        synchronized (roleCache) { roleCache.put(key, new CacheEntry(now, now + ROLE_CACHE_TTL_TICKS, context)); }
        return context;
    }

    public Optional<CapitalIdentity> capitalByMcaVillageId(int villageId) {
        if (!villageLookupAvailable()) return Optional.empty();
        try {
            Object capital = getCapitalByVillageId.invoke(null, Integer.valueOf(villageId));
            return capital == null ? Optional.empty() : identity(capital);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnQueryOnce("capital identity", exception);
            return Optional.empty();
        }
    }

    public void clearCache() { synchronized (roleCache) { roleCache.clear(); } }
    public int cachedRoleCount() { synchronized (roleCache) { return roleCache.size(); } }

    private PoliticalContext queryPoliticalContext(ServerLevel level, UUID entityId) {
        try {
            Object capital = findCapitalForEntity.invoke(null, level, entityId);
            if (capital == null) return PoliticalContext.notPolitical();
            String title = (String) getDisplayTitle.invoke(null, level, capital, entityId);
            boolean guard = Boolean.TRUE.equals(isRoyalGuard.invoke(capital, entityId));
            PoliticalRole role = PoliticalRole.fromExactTitle(title, guard);
            Optional<CapitalIdentity> identity = identity(capital);
            if (identity.isEmpty()) return PoliticalContext.queryFailed("MCA Capitals returned a role without a complete capital identity");
            CapitalIdentity value = identity.get();
            String detail = role == PoliticalRole.UNKNOWN ? "unmapped title from exact MCA Capitals adapter: " + title : "verified MCA Capitals role";
            return new PoliticalContext(PoliticalLookupStatus.AVAILABLE, role, Optional.of(value.capitalId()),
                    OptionalInt.of(value.mcaVillageId()), Optional.of(value.state()), Optional.ofNullable(title), detail);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnQueryOnce("political role", exception);
            return PoliticalContext.queryFailed("MCA Capitals role query failed: " + safeMessage(rootCause(exception)));
        }
    }

    private Optional<CapitalIdentity> identity(Object capital) throws ReflectiveOperationException {
        Object capitalIdValue = getCapitalId.invoke(capital);
        Object villageIdValue = getVillageId.invoke(capital);
        Object stateValue = getState.invoke(capital);
        if (!(capitalIdValue instanceof UUID capitalId) || !(villageIdValue instanceof Integer villageId)
                || !(stateValue instanceof Enum<?> state)) return Optional.empty();
        return Optional.of(new CapitalIdentity(capitalId, villageId, state.name()));
    }

    private void warnQueryOnce(String operation, Exception exception) {
        if (queryFailureLogged.compareAndSet(false, true)) {
            Throwable root = rootCause(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mcacapitals] {} query failed; political context will be unknown: {}",
                    operation, safeMessage(root));
        }
    }

    private synchronized void reset() {
        resetMembers(); identityReads.reset(); roleReads.reset(); villageLookup.reset(); queryFailureLogged.set(false); clearCache();
        statusDetail = "not initialized";
    }

    private void resetMembers() {
        ready = false; capitalRecordClass = null; findCapitalForEntity = null; getCapitalByVillageId = null;
        getDisplayTitle = null; getCapitalId = null; getVillageId = null; getState = null; isRoyalGuard = null;
    }

    private void failAll(String reason) {
        identityReads.fail(reason); roleReads.fail(reason); villageLookup.fail(reason); ready = false;
    }

    private static Throwable rootCause(Throwable throwable) {
        if (throwable instanceof InvocationTargetException invocation && invocation.getCause() != null) return invocation.getCause();
        return throwable;
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private record RoleCacheKey(int serverIdentity, String dimension, UUID entityId) {}
    private record CacheEntry(long createdAt, long expiresAt, PoliticalContext context) {}
}
