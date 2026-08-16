package com.darkfolklore.core.contracts;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MonsterContractTest {
    @Test
    void completeInvestigationWorkflowRequiresEvidenceAndHunt() {
        MonsterContract contract = new MonsterContract(UUID.randomUUID(), UUID.randomUUID(),
                "darkfolklore:wendigo", 10_000);
        assertTrue(contract.start());
        assertTrue(contract.addEvidence(EvidenceType.BLOOD, 2));
        assertEquals(ContractStatus.INVESTIGATING, contract.status());
        assertTrue(contract.addEvidence(EvidenceType.FOOTPRINT, 2));
        assertEquals(ContractStatus.IDENTIFIED, contract.status());
        assertTrue(contract.markHunted());
        assertTrue(contract.complete());
        assertEquals(ContractStatus.COMPLETE, contract.status());
    }

    @Test
    void expiredContractCannotAdvance() {
        MonsterContract contract = new MonsterContract(UUID.randomUUID(), UUID.randomUUID(),
                "darkfolklore:vampire", 100);
        assertTrue(contract.expire(100));
        assertFalse(contract.start());
    }

    @Test
    void identifiedContractCanRetainPostIdentificationOccultResearch() {
        MonsterContract contract = new MonsterContract(UUID.randomUUID(), UUID.randomUUID(),
                "darkfolklore:vampire", 10_000);
        assertTrue(contract.start());
        assertTrue(contract.addEvidence(EvidenceType.BLOOD, 2));
        assertTrue(contract.addEvidence(EvidenceType.BITE_MARK, 2));
        assertEquals(ContractStatus.IDENTIFIED, contract.status());
        assertTrue(contract.recordEvidence(EvidenceType.SOUL_ECHO));
        assertTrue(contract.evidence().contains(EvidenceType.SOUL_ECHO));
        assertEquals(ContractStatus.IDENTIFIED, contract.status());
    }

    @Test
    void conclusiveModeRecordsThresholdEvidenceWithoutAutoIdentifying() {
        MonsterContract contract = new MonsterContract(UUID.randomUUID(), UUID.randomUUID(),
                "darkfolklore:vampire", 10_000);
        assertTrue(contract.start());

        assertTrue(ContractEvidenceProgression.recordForMode(contract, EvidenceType.BLOOD, 2, true));
        assertTrue(ContractEvidenceProgression.recordForMode(contract, EvidenceType.BITE_MARK, 2, true));

        assertEquals(2, contract.evidence().size());
        assertEquals(ContractStatus.INVESTIGATING, contract.status(),
                "only the hypothesis engine may identify in conclusive mode");
    }

    @Test
    void legacyModeStillIdentifiesAtDistinctEvidenceThreshold() {
        MonsterContract contract = new MonsterContract(UUID.randomUUID(), UUID.randomUUID(),
                "darkfolklore:wendigo", 10_000);
        assertTrue(contract.start());

        assertTrue(ContractEvidenceProgression.recordForMode(contract, EvidenceType.BLOOD, 2, false));
        assertTrue(ContractEvidenceProgression.recordForMode(contract, EvidenceType.FOOTPRINT, 2, false));

        assertEquals(ContractStatus.IDENTIFIED, contract.status());
    }

    @Test
    void duplicateEvidenceCannotMutateStatusWhileReturningFalse() {
        MonsterContract contract = new MonsterContract(UUID.randomUUID(), UUID.randomUUID(),
                "darkfolklore:wendigo", 10_000);
        assertTrue(contract.start());
        assertTrue(contract.recordEvidence(EvidenceType.BLOOD));

        assertFalse(contract.addEvidence(EvidenceType.BLOOD, 1));
        assertEquals(ContractStatus.INVESTIGATING, contract.status());
    }

    @Test
    void targetConceptMustBeAnExplicitNonEmptyResourceId() {
        assertThrows(IllegalArgumentException.class, () -> new MonsterContract(
                UUID.randomUUID(), UUID.randomUUID(), "vampire", 10_000));
        assertThrows(IllegalArgumentException.class, () -> new MonsterContract(
                UUID.randomUUID(), UUID.randomUUID(), "darkfolklore:", 10_000));
        assertDoesNotThrow(() -> new MonsterContract(
                UUID.randomUUID(), UUID.randomUUID(), "darkfolklore:vampire", 10_000));
    }
}
