package com.darkfolklore.core.living.casebook;

import com.darkfolklore.core.contracts.*;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.investigation.PreparationAssessment;
import com.darkfolklore.core.living.LivingFolkloreConfig;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.persistence.InvestigationSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.Optional;
import java.util.UUID;

public final class CasebookLifecycleEngine {
    public static final CasebookLifecycleEngine INSTANCE=new CasebookLifecycleEngine();
    private CasebookLifecycleEngine() {}

    @SubscribeEvent public void onTick(ServerTickEvent.Post event){
        if(!LivingFolkloreConfig.LIVING_FOLKLORE.get()||event.getServer().getTickCount()%20!=0)return;
        LivingFolkloreSavedData cases=LivingFolkloreSavedData.get(event.getServer());
        FolkloreSavedData society=FolkloreSavedData.get(event.getServer());long now=event.getServer().overworld().getGameTime();
        for(ContractAssignment assignment:society.contracts()){
            ServerPlayer player=event.getServer().getPlayerList().getPlayer(assignment.player());
            InvestigationCaseRecord value=cases.caseForContract(assignment.contract().id()).orElse(null);
            if(value==null&&player!=null&&!assignment.contract().status().terminal()){
                Optional<UUID> story=InvestigationSavedData.get(event.getServer()).caseLink(assignment.contract().id()).flatMap(link->link.storyId());
                value=CasebookService.INSTANCE.open(player,assignment,story);
            }
            if(value==null||value.stage().terminal())continue;
            for(var type:assignment.contract().evidence())if(!value.evidence().contains(type)){
                value.addEvidence(type,now);
                value.addNote(new CaseNote(now,type.name().equals("TESTIMONY")?CaseNoteKind.TESTIMONY:CaseNoteKind.EVIDENCE,
                        "Case evidence recorded: "+type.name().toLowerCase()+".",Optional.empty(),Optional.of(type),Optional.empty(),
                        type.name().equals("TESTIMONY")?.55F:1.0F),LivingFolkloreConfig.CASE_NOTES.get());
            }
            ContractStatus status=assignment.contract().status();
            if(status==ContractStatus.IDENTIFIED){
                if(value.stage().ordinal()<CaseStage.IDENTIFIED.ordinal())value.identify(assignment.contract().targetConcept(),now);
                if(player!=null&&value.stage().ordinal()<CaseStage.PREPARED.ordinal()){
                    var profile=FolkloreDataManager.INSTANCE.investigationProfile(assignment.contract().targetConcept()).orElse(null);
                    if(profile!=null){
                        PreparationAssessment prepared=PreparationAssessment.evaluate(player,profile);
                        if(prepared.prepared()){
                            value.advance(CaseStage.PREPARED,now);
                            value.addNote(new CaseNote(now,CaseNoteKind.PREPARATION,"Studied countermeasures are present; the hunt is prepared.",Optional.empty(),Optional.empty(),Optional.empty(),1.0F),LivingFolkloreConfig.CASE_NOTES.get());
                        }
                    }
                }
            } else if(status==ContractStatus.HUNTED)value.advance(CaseStage.HUNTED,now);
            else if(status==ContractStatus.COMPLETE)value.advance(CaseStage.RESOLVED,now);
            else if(status==ContractStatus.EXPIRED)value.advance(CaseStage.EXPIRED,now);
            cases.changed(value);
        }
        cases.prune(now,LivingFolkloreConfig.CASE_RETENTION.get());
    }
}
