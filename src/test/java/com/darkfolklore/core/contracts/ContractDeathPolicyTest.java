package com.darkfolklore.core.contracts;

import com.darkfolklore.core.investigation.IncidentFact;
import com.darkfolklore.core.investigation.InvestigationCaseLink;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractDeathPolicyTest {
    @Test
    void issuerFallbackRequiresTheExactIssuerAndANonterminalContract() {
        UUID issuer = UUID.randomUUID();
        MonsterContract contract = investigatingContract(issuer);
        assertTrue(ContractDeathPolicy.allowIssuerFallback(contract, issuer));
        assertFalse(ContractDeathPolicy.allowIssuerFallback(contract, UUID.randomUUID()));
    }

    @Test
    void culpritFallbackRequiresTheExactConfirmedCulpritAndDoesNotReplaceACompletedHunt() {
        MonsterContract contract = investigatingContract(UUID.randomUUID());
        UUID culprit = UUID.randomUUID();
        InvestigationCaseLink link = InvestigationCaseLink.fromStory(UUID.randomUUID(),
                new IncidentFact(Optional.of(culprit), "eidolon_repraised:wraith", 1L));

        assertTrue(ContractDeathPolicy.allowCulpritFallback(contract, link, culprit, null));
        assertFalse(ContractDeathPolicy.allowCulpritFallback(contract, link, UUID.randomUUID(), null));
        assertFalse(ContractDeathPolicy.allowCulpritFallback(contract, link, culprit, contract.id()));
    }

    private static MonsterContract investigatingContract(UUID issuer) {
        MonsterContract contract = new MonsterContract(UUID.randomUUID(), issuer,
                "darkfolklore:wraith", 10_000L);
        contract.start();
        return contract;
    }
}
