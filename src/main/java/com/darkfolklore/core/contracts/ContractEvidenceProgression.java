package com.darkfolklore.core.contracts;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.living.LivingFolkloreConfig;
import com.darkfolklore.core.living.casebook.CasebookService;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Single authority for adding investigation evidence to a contract.
 *
 * <p>When Living Folklore conclusive identification is enabled, reaching the distinct-evidence threshold is only
 * a prerequisite: evidence is recorded without changing status and the Casebook hypothesis engine decides whether
 * the evidence actually identifies the hidden target. When that overlay is disabled, the legacy threshold behavior
 * remains available for backwards-compatible/simple configurations.</p>
 */
public final class ContractEvidenceProgression {
    private ContractEvidenceProgression() {}

    public static Result record(ServerPlayer player, FolkloreSavedData data,
                                ContractAssignment assignment, EvidenceType type) {
        ContractStatus before = assignment.contract().status();
        boolean conclusiveMode = conclusiveModeEnabled();
        boolean changed = recordForMode(assignment.contract(), type, assignment.requiredDistinctClues(), conclusiveMode);
        if (!changed) return new Result(false, false, conclusiveMode);

        data.putContract(assignment);
        if (conclusiveMode && assignment.contract().status() == ContractStatus.INVESTIGATING) {
            CasebookService.INSTANCE.tryIdentify(player, data, assignment);
        }
        boolean identifiedNow = before != ContractStatus.IDENTIFIED
                && assignment.contract().status() == ContractStatus.IDENTIFIED;
        return new Result(true, identifiedNow, conclusiveMode);
    }

    public static boolean conclusiveModeEnabled() {
        return CasebookService.enabled() && LivingFolkloreConfig.CONCLUSIVE_IDENTIFICATION.get();
    }

    /** Pure policy seam used by unit tests and by the runtime wrapper above. */
    static boolean recordForMode(MonsterContract contract, EvidenceType type,
                                 int requiredDistinctClues, boolean conclusiveMode) {
        return conclusiveMode
                ? contract.recordEvidence(type)
                : contract.addEvidence(type, requiredDistinctClues);
    }

    public record Result(boolean changed, boolean identifiedNow, boolean conclusiveMode) {}
}
