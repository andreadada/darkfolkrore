package com.darkfolklore.core.compat;

import java.util.Collection;

/** Pure routing policy that keeps MCA provider facts authoritative and fail-closed. */
public final class SupernaturalFactResolver {
    private SupernaturalFactResolver() {}

    public static FactResult resolveMca(CompatibilityStatus authorityStatus, FactResult providerResult) {
        return switch (authorityStatus) {
            case ACTIVE -> providerResult == FactResult.NOT_APPLICABLE ? FactResult.UNKNOWN : providerResult;
            case DISABLED -> FactResult.NOT_APPLICABLE;
            case UNTESTED_VERSION, PARTIAL, UNSUPPORTED, ERROR -> FactResult.UNKNOWN;
        };
    }

    public static FactResult resolveGeneric(Collection<FactResult> results) {
        boolean hadFalse = false;
        boolean hadUnknown = false;
        for (FactResult result : results) {
            if (result == FactResult.TRUE) return FactResult.TRUE;
            if (result == FactResult.UNKNOWN) hadUnknown = true;
            if (result == FactResult.FALSE) hadFalse = true;
        }
        if (hadUnknown) return FactResult.UNKNOWN;
        return hadFalse ? FactResult.FALSE : FactResult.NOT_APPLICABLE;
    }
}
