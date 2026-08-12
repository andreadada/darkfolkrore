package com.darkfolklore.core.contracts;

import com.darkfolklore.core.investigation.InvestigationCaseLink;

import java.util.UUID;

/** Pure policy for death-authorized continuity changes. Called only after final death confirmation. */
public final class ContractDeathPolicy {
    private ContractDeathPolicy() {}

    public static boolean allowIssuerFallback(MonsterContract contract, UUID dead) {
        return !contract.status().terminal() && contract.issuer().equals(dead);
    }

    public static boolean allowCulpritFallback(MonsterContract contract, InvestigationCaseLink link,
                                               UUID dead, UUID huntedContractId) {
        return !contract.status().terminal()
                && contract.status() != ContractStatus.HUNTED
                && link != null
                && link.culpritId().filter(dead::equals).isPresent()
                && !contract.id().equals(huntedContractId);
    }
}
