package com.darkfolklore.core.compat.mcacapitals;

import com.darkfolklore.core.DarkFolkloreCore;
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
 *
 * <p>Role reflection is resolved at activation and role results are cached for 20 game ticks in a 1,024-entry LRU.
 * Cached values contain no MCA Capitals objects and therefore cannot leak its implementation state.</p>
 */
public final class McaCapitalsCompat {
    public static final String MOD_ID = "mcacapitals";
    public static final String TESTED_VERSION = "1.1.0";
    public static final int ROLE_CACHE_MAX_ENTRIES = 1024;
    public static final long ROLE_CACHE_TTL_TICKS = 20;

    private final AtomicBoolean queryFailureLogged = new AtomicBoolean();
    private final Map<RoleCacheKey, CacheEntry> roleCache = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<RoleCacheKey, CacheEntry> eldest) {
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
            statusDetail = "disabled: requires exact MCA Capitals " + TESTED_VERSION + ", found " + actualVersion;
            return false;
        }
        try {
            ClassLoader loader = McaCapitalsCompat.class.getClassLoader();
            Class<?> titleResolver = Class.forName(
                    "com.majesttyx.mcacapitals.capital.CapitalTitleResolver", false, loader);
            Class<?> manager = Class.forName("com.majesttyx.mcacapitals.capital.CapitalManager", false, loader);
            capitalRecordClass = Class.forName("com.majesttyx.mcacapitals.capital.CapitalRecord", false, loader);

            findCapitalForEntity = titleResolver.getMethod("findCapitalForEntity", ServerLevel.class, UUID.class);
            getCapitalByVillageId = manager.getMethod("getCapitalByVillageId", Integer.class);
            getDisplayTitle = titleResolver.getMethod("getDisplayTitle", ServerLevel.class,
                    capitalRecordClass, UUID.class);
            getCapitalId = capitalRecordClass.getMethod("getCapitalId");
            getVillageId = capitalRecordClass.getMethod("getVillageId");
            getState = capitalRecordClass.getMethod("getState");
            isRoyalGuard = capitalRecordClass.getMethod("isRoyalGuard", UUID.class);
            ready = true;
            statusDetail = "active: exact MCA Capitals " + TESTED_VERSION + " political bridge";
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            resetMembers();
            statusDetail = "disabled: MCA Capitals " + TESTED_VERSION + " signature mismatch ("
                    + exception.getClass().getSimpleName() + ": " + safeMessage(exception) + ")";
            return false;
        }
    }

    public boolean isReady() {
        return ready;
    }

    public String statusDetail() {
        return statusDetail;
    }

    public PoliticalContext politicalContext(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return PoliticalContext.queryFailed("entity is not on a logical server");
        }
        return politicalContext(level, entity.getUUID());
    }

    public PoliticalContext politicalContext(ServerLevel level, UUID entityId) {
        if (!ready) return PoliticalContext.disabled(statusDetail);
        long now = level.getGameTime();
        RoleCacheKey key = new RoleCacheKey(System.identityHashCode(level.getServer()),
                level.dimension().location().toString(), entityId);
        synchronized (roleCache) {
            CacheEntry cached = roleCache.get(key);
            if (cached != null && now >= cached.createdAt && now < cached.expiresAt) return cached.context;
        }

        PoliticalContext context = queryPoliticalContext(level, entityId);
        synchronized (roleCache) {
            roleCache.put(key, new CacheEntry(now, now + ROLE_CACHE_TTL_TICKS, context));
        }
        return context;
    }

    /** Queries a capital by the MCA village id stored by MCA Capitals; it performs no spatial inference. */
    public Optional<CapitalIdentity> capitalByMcaVillageId(int villageId) {
        if (!ready) return Optional.empty();
        try {
            Object capital = getCapitalByVillageId.invoke(null, Integer.valueOf(villageId));
            return capital == null ? Optional.empty() : identity(capital);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnQueryOnce("capital identity", exception);
            return Optional.empty();
        }
    }

    public void clearCache() {
        synchronized (roleCache) {
            roleCache.clear();
        }
    }

    public int cachedRoleCount() {
        synchronized (roleCache) {
            return roleCache.size();
        }
    }

    private PoliticalContext queryPoliticalContext(ServerLevel level, UUID entityId) {
        try {
            Object capital = findCapitalForEntity.invoke(null, level, entityId);
            if (capital == null) return PoliticalContext.notPolitical();
            String title = (String) getDisplayTitle.invoke(null, level, capital, entityId);
            boolean guard = Boolean.TRUE.equals(isRoyalGuard.invoke(capital, entityId));
            PoliticalRole role = PoliticalRole.fromExactTitle(title, guard);
            Optional<CapitalIdentity> identity = identity(capital);
            if (identity.isEmpty()) {
                return PoliticalContext.queryFailed("MCA Capitals returned a role without a complete capital identity");
            }
            CapitalIdentity value = identity.get();
            String detail = role == PoliticalRole.UNKNOWN
                    ? "unmapped title from exact MCA Capitals adapter: " + title
                    : "verified MCA Capitals role";
            return new PoliticalContext(PoliticalLookupStatus.AVAILABLE, role, Optional.of(value.capitalId()),
                    OptionalInt.of(value.mcaVillageId()), Optional.of(value.state()), Optional.ofNullable(title), detail);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnQueryOnce("political role", exception);
            return PoliticalContext.queryFailed("MCA Capitals role query failed: "
                    + safeMessage(rootCause(exception)));
        }
    }

    private Optional<CapitalIdentity> identity(Object capital) throws ReflectiveOperationException {
        Object capitalIdValue = getCapitalId.invoke(capital);
        Object villageIdValue = getVillageId.invoke(capital);
        Object stateValue = getState.invoke(capital);
        if (!(capitalIdValue instanceof UUID capitalId) || !(villageIdValue instanceof Integer villageId)
                || !(stateValue instanceof Enum<?> state)) {
            return Optional.empty();
        }
        return Optional.of(new CapitalIdentity(capitalId, villageId, state.name()));
    }

    private void warnQueryOnce(String operation, Exception exception) {
        if (queryFailureLogged.compareAndSet(false, true)) {
            Throwable root = rootCause(exception);
            DarkFolkloreCore.LOGGER.warn(
                    "[compat/mcacapitals] {} query failed; political context will be unknown: {}",
                    operation, safeMessage(root));
        }
    }

    private synchronized void reset() {
        resetMembers();
        queryFailureLogged.set(false);
        clearCache();
        statusDetail = "not initialized";
    }

    private void resetMembers() {
        ready = false;
        capitalRecordClass = null;
        findCapitalForEntity = null;
        getCapitalByVillageId = null;
        getDisplayTitle = null;
        getCapitalId = null;
        getVillageId = null;
        getState = null;
        isRoyalGuard = null;
    }

    private static Throwable rootCause(Throwable throwable) {
        if (throwable instanceof InvocationTargetException invocation && invocation.getCause() != null) {
            return invocation.getCause();
        }
        return throwable;
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private record RoleCacheKey(int serverIdentity, String dimension, UUID entityId) {}

    private record CacheEntry(long createdAt, long expiresAt, PoliticalContext context) {}
}
