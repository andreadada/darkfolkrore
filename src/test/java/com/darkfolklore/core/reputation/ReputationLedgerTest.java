package com.darkfolklore.core.reputation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReputationLedgerTest {
    @Test
    void reputationClampsToSupportedRange() {
        ReputationLedger ledger = new ReputationLedger();
        assertEquals(100, ledger.add(ReputationFaction.HUNTERS, 500));
        assertEquals(-100, ledger.add(ReputationFaction.HUNTERS, -500));
    }
}
