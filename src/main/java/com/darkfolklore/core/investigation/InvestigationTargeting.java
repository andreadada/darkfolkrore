package com.darkfolklore.core.investigation;

import com.darkfolklore.core.contracts.ContractAssignment;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.society.SecretFacts;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;

/** Shared factual target matching for tracking, prepared hunts and contract completion. */
public final class InvestigationTargeting {
    private InvestigationTargeting() {}

    public static String canonicalConcept(LivingEntity entity) {
        String registry = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return FolkloreDataManager.INSTANCE.canonical().resolve(registry)
                .map(value -> value.concept()).orElseGet(() -> SecretFacts.canonicalConcept(entity));
    }

    public static boolean matches(ContractAssignment assignment, LivingEntity entity,
                                  InvestigationCaseLink link) {
        if (!canonicalConcept(entity).equals(assignment.contract().targetConcept())) return false;
        if (link == null || link.culpritId().isEmpty()) return true;
        return entity.getUUID().equals(link.culpritId().get()) || link.culpritFallbackAllowed();
    }
}
