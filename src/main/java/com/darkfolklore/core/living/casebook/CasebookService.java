package com.darkfolklore.core.living.casebook;

import com.darkfolklore.core.contracts.*;
import com.darkfolklore.core.investigation.Hypothesis;
import com.darkfolklore.core.investigation.HypothesisEngine;
import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.living.LivingFolkloreConfig;
import com.darkfolklore.core.magic.MagicTradition;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Player-facing case orchestration. Rankings are derived only from evidence already held by the contract. */
public final class CasebookService {
    public static final CasebookService INSTANCE = new CasebookService();
    private CasebookService() {}

    public InvestigationCaseRecord open(ServerPlayer player, ContractAssignment assignment, Optional<UUID> storyId) {
        LivingFolkloreSavedData data = LivingFolkloreSavedData.get(player.getServer());
        InvestigationCaseRecord value = data.create(player.getUUID(), assignment.contract().id(), storyId,
                assignment.investigationCenter(), player.level().getGameTime(), assignment.contract().expiresAt(),
                LivingFolkloreConfig.CASES_PER_PLAYER.get());
        value.addNote(new CaseNote(player.level().getGameTime(), CaseNoteKind.SOCIAL,
                "A local supernatural incident was accepted for investigation.", Optional.of(assignment.contract().issuer()),
                Optional.empty(), Optional.empty(), 1.0F), LivingFolkloreConfig.CASE_NOTES.get());
        data.changed(value);
        return value;
    }

    public void record(ServerPlayer player, ContractAssignment assignment, EvidenceType type, CaseNoteKind kind,
                       String detail, Optional<UUID> source, float confidence, Optional<MagicTradition> tradition) {
        if (!enabled()) return;
        LivingFolkloreSavedData data = LivingFolkloreSavedData.get(player.getServer());
        InvestigationCaseRecord value = data.caseForContract(assignment.contract().id()).orElseGet(() -> open(player,assignment,Optional.empty()));
        value.addEvidence(type, player.level().getGameTime());
        value.addNote(new CaseNote(player.level().getGameTime(),kind,detail,source,Optional.of(type),tradition,confidence), LivingFolkloreConfig.CASE_NOTES.get());
        leading(assignment).ifPresent(first -> value.addNote(new CaseNote(player.level().getGameTime(),CaseNoteKind.HYPOTHESIS,
                "Leading hypothesis: " + first.concept() + " (support " + Math.round(first.confidence()*100) + "%).",
                Optional.empty(),Optional.empty(),Optional.empty(),first.confidence()), LivingFolkloreConfig.CASE_NOTES.get()));
        data.changed(value);
    }

    public boolean tryIdentify(ServerPlayer player, FolkloreSavedData society, ContractAssignment assignment) {
        if (!enabled() || !LivingFolkloreConfig.CONCLUSIVE_IDENTIFICATION.get() || assignment.contract().status()!=ContractStatus.INVESTIGATING) return false;
        List<Hypothesis> ranked = HypothesisEngine.rank(assignment.contract().evidence());
        if (ranked.isEmpty() || assignment.contract().evidence().size() < assignment.requiredDistinctClues()) return false;
        Hypothesis first = ranked.getFirst();
        float second = ranked.size()>1 ? ranked.get(1).confidence() : 0.0F;
        float margin = Math.max(0.0F, first.confidence()-second);
        boolean conclusive = first.confidence() >= LivingFolkloreConfig.IDENTIFICATION_CONFIDENCE.get()
                && margin >= LivingFolkloreConfig.IDENTIFICATION_MARGIN.get();
        LivingFolkloreSavedData caseData = LivingFolkloreSavedData.get(player.getServer());
        InvestigationCaseRecord value = caseData.caseForContract(assignment.contract().id()).orElseGet(() -> open(player,assignment,Optional.empty()));
        value.addNote(new CaseNote(player.level().getGameTime(),CaseNoteKind.HYPOTHESIS,
                (conclusive ? "Conclusive" : "Contested") + " evidence currently favors " + first.concept() + ".",
                Optional.empty(),Optional.empty(),Optional.empty(),first.confidence()), LivingFolkloreConfig.CASE_NOTES.get());
        caseData.changed(value);
        if (!conclusive || !first.concept().equals(assignment.contract().targetConcept()) || !assignment.contract().identify()) return false;
        society.putContract(assignment);
        value.identify(first.concept(),player.level().getGameTime());
        caseData.changed(value);
        return true;
    }

    public void prepared(MinecraftServer server, ContractAssignment assignment, String detail) { advance(server,assignment,CaseStage.PREPARED,CaseNoteKind.PREPARATION,detail); }
    public void hunted(MinecraftServer server, ContractAssignment assignment) { advance(server,assignment,CaseStage.HUNTED,CaseNoteKind.RESOLUTION,"The identified incident target was defeated."); }
    public void resolved(MinecraftServer server, ContractAssignment assignment) { advance(server,assignment,CaseStage.RESOLVED,CaseNoteKind.RESOLUTION,"The investigation was resolved."); }

    private void advance(MinecraftServer server, ContractAssignment assignment, CaseStage stage, CaseNoteKind kind, String detail) {
        LivingFolkloreSavedData data=LivingFolkloreSavedData.get(server);
        data.caseForContract(assignment.contract().id()).ifPresent(value->{ long now=server.overworld().getGameTime();
            if(value.advance(stage,now)) value.addNote(new CaseNote(now,kind,detail,Optional.empty(),Optional.empty(),Optional.empty(),1.0F),LivingFolkloreConfig.CASE_NOTES.get());
            data.changed(value); });
    }

    public String summary(ContractAssignment assignment) {
        List<Hypothesis> ranked=HypothesisEngine.rank(assignment.contract().evidence());
        if(ranked.isEmpty()) return "Hypotheses: insufficient evidence.";
        Hypothesis first=ranked.getFirst();
        String next=ranked.size()>1 ? ", next " + ranked.get(1).concept() + " " + Math.round(ranked.get(1).confidence()*100) + "%" : "";
        return "Hypotheses: " + first.concept() + " " + Math.round(first.confidence()*100) + "%" + next;
    }

    public Optional<Hypothesis> leading(ContractAssignment assignment) {
        List<Hypothesis> ranked=HypothesisEngine.rank(assignment.contract().evidence());
        return ranked.isEmpty()?Optional.empty():Optional.of(ranked.getFirst());
    }

    public static boolean enabled() { return LivingFolkloreConfig.LIVING_FOLKLORE.get() && LivingFolkloreConfig.CASEBOOK.get(); }
}
