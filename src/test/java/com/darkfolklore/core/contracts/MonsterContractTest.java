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
}
