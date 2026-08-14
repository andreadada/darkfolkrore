package com.darkfolklore.core.living.casebook;

import com.darkfolklore.core.api.event.RumorSpreadEvent;
import com.darkfolklore.core.investigation.InvestigationCaseLink;
import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.living.LivingFolkloreConfig;
import com.darkfolklore.core.persistence.InvestigationSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import java.util.Optional;

/** A rumor reaches the casebook only when it reaches the player and is tied to the exact case subject. */
public final class CasebookSocialEngine {
    public static final CasebookSocialEngine INSTANCE = new CasebookSocialEngine();
    private CasebookSocialEngine() {}

    @SubscribeEvent
    public void onRumor(RumorSpreadEvent event) {
        if (!LivingFolkloreConfig.LIVING_FOLKLORE.get() || !LivingFolkloreConfig.CASEBOOK.get()) return;
        MinecraftServer server=ServerLifecycleHooks.getCurrentServer();
        if(server==null) return;
        ServerPlayer player=server.getPlayerList().getPlayer(event.recipient());
        if(player==null) return;
        LivingFolkloreSavedData data=LivingFolkloreSavedData.get(server);
        InvestigationCaseRecord value=data.activeCase(player.getUUID()).orElse(null);
        if(value==null) return;
        InvestigationCaseLink link=InvestigationSavedData.get(server).caseLink(value.contractId()).orElse(null);
        if(link==null || link.culpritId().filter(event.subject()::equals).isEmpty()) return;
        value.addNote(new CaseNote(server.overworld().getGameTime(),CaseNoteKind.SOCIAL,
                "Relevant rumor received; it remains testimony rather than confirmed fact.",Optional.of(event.sender()),
                Optional.of(EvidenceType.TESTIMONY),Optional.empty(),event.knowledge().confidence()), LivingFolkloreConfig.CASE_NOTES.get());
        data.changed(value);
    }
}
