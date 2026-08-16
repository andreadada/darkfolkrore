package com.darkfolklore.core.living.casebook;

import com.darkfolklore.core.persistence.WorldPosition;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InvestigationCaseRecordTest {
    private static InvestigationCaseRecord fresh() {
        return new InvestigationCaseRecord(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Optional.empty(),
                CaseOrigin.CONTRACT, new WorldPosition("minecraft:overworld", 0, 64, 0), 100L, 10_000L);
    }

    @Test
    void identificationRequiresARealNamespacedConceptAndCannotBeRewritten() {
        InvestigationCaseRecord record = fresh();
        assertFalse(record.identify("darkfolklore:", 200L));
        assertFalse(record.identify("vampire", 200L));
        assertEquals(CaseStage.INVESTIGATING, record.stage());

        assertTrue(record.identify("darkfolklore:vampire", 200L));
        assertEquals(CaseStage.IDENTIFIED, record.stage());
        assertEquals("darkfolklore:vampire", record.identifiedConcept().orElseThrow());

        assertFalse(record.identify("darkfolklore:werewolf", 300L));
        assertEquals("darkfolklore:vampire", record.identifiedConcept().orElseThrow());
    }

    @Test
    void terminalCaseCannotBeIdentifiedRetroactively() {
        InvestigationCaseRecord record = fresh();
        assertTrue(record.advance(CaseStage.EXPIRED, 300L));
        assertFalse(record.identify("darkfolklore:vampire", 400L));
        assertTrue(record.identifiedConcept().isEmpty());
        assertEquals(CaseStage.EXPIRED, record.stage());
    }

    @Test
    void restoreDropsMalformedIdentifiedConcept() {
        InvestigationCaseRecord record = fresh();
        record.restore(CaseStage.IDENTIFIED, null, null, "darkfolklore:", 500L);
        assertTrue(record.identifiedConcept().isEmpty());
    }
}
