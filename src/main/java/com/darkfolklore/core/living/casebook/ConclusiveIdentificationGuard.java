package com.darkfolklore.core.living.casebook;

import com.darkfolklore.core.contracts.*;
import com.darkfolklore.core.investigation.HypothesisEngine;
import com.darkfolklore.core.living.LivingFolkloreConfig;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class ConclusiveIdentificationGuard {
    public static final ConclusiveIdentificationGuard INSTANCE=new ConclusiveIdentificationGuard();
    private ConclusiveIdentificationGuard() {}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void onBlock(PlayerInteractEvent.RightClickBlock e){if(e.getEntity() instanceof ServerPlayer p)reconcile(p);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void onEntity(PlayerInteractEvent.EntityInteract e){if(e.getEntity() instanceof ServerPlayer p)reconcile(p);}

    private void reconcile(ServerPlayer player){
        if(!LivingFolkloreConfig.LIVING_FOLKLORE.get()||!LivingFolkloreConfig.CONCLUSIVE_IDENTIFICATION.get())return;
        FolkloreSavedData data=FolkloreSavedData.get(player.getServer());
        ContractAssignment assignment=data.activeContract(player.getUUID()).orElse(null);
        if(assignment==null||assignment.contract().status()!=ContractStatus.IDENTIFIED)return;
        var ranked=HypothesisEngine.rank(assignment.contract().evidence());
        boolean valid=IdentificationPolicy.conclusive(ranked,assignment.contract().evidence().size(),assignment.requiredDistinctClues(),assignment.contract().targetConcept());
        LivingFolkloreSavedData cases=LivingFolkloreSavedData.get(player.getServer());
        InvestigationCaseRecord record=cases.caseForContract(assignment.contract().id()).orElse(null);
        if(valid){if(record!=null){record.identify(assignment.contract().targetConcept(),player.level().getGameTime());cases.changed(record);}return;}
        var observed=assignment.contract().evidence(); assignment.contract().restore(ContractStatus.INVESTIGATING,observed); data.putContract(assignment);
        if(record!=null){record.addNote(new CaseNote(player.level().getGameTime(),CaseNoteKind.HYPOTHESIS,"Competing explanations remain after the clue threshold.",java.util.Optional.empty(),java.util.Optional.empty(),java.util.Optional.empty(),ranked.isEmpty()?0.0F:ranked.getFirst().confidence()),LivingFolkloreConfig.CASE_NOTES.get());cases.changed(record);}
        player.displayClientMessage(Component.literal("The evidence is still ambiguous. Find a discriminating clue or use occult analysis."),true);
    }
}
