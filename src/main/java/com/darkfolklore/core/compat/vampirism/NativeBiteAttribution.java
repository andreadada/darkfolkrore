package com.darkfolklore.core.compat.vampirism;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Pure exact-event correlation policy for provider-owned native MCA bites. */
public final class NativeBiteAttribution {
    private NativeBiteAttribution() {}

    public static boolean confirmed(Attempt attempt, UUID predator, UUID target, boolean biteReadyAfter) {
        return attempt != null && attempt.predator().equals(predator) && attempt.target().equals(target)
                && attempt.biteReadyBefore() && !biteReadyAfter;
    }

    public record Attempt(UUID predator, UUID target, boolean biteReadyBefore) {}

    /** Short-lived event bracket cache; entries are consumed at LOWEST and weak keys contain abandoned events. */
    static final class PendingAttempts<K> {
        private final Map<K, Attempt> attempts = Collections.synchronizedMap(new WeakHashMap<>());

        void capture(K event, Attempt attempt) {
            attempts.put(event, attempt);
        }

        Attempt consume(K event) {
            return attempts.remove(event);
        }

        void clear() {
            attempts.clear();
        }

        int size() {
            return attempts.size();
        }
    }
}
