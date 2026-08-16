package com.darkfolklore.core.living.forensics;

import com.darkfolklore.core.contracts.ContractAssignment;
import com.darkfolklore.core.contracts.ContractEvidenceProgression;
import com.darkfolklore.core.contracts.ContractStatus;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.investigation.EvidenceRecord;
import com.darkfolklore.core.investigation.InvestigationCaseLink;
import com.darkfolklore.core.investigation.InvestigationTargeting;
import com.darkfolklore.core.knowledge.lore.LoreEngine;
import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.living.LivingFolkloreConfig;
import com.darkfolklore.core.living.casebook.CaseNoteKind;
import com.darkfolklore.core.living.casebook.CasebookService;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.persistence.InvestigationSavedData;
import com.darkfolklore.core.society.story.PersistentStory;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Comparator;
import java.util.Optional;

/** Provider-neutral blood forensics. It derives a resonance clue, never a hidden identity fact. */
public final class BloodForensicsEngine {
    public static final BloodForensicsEngine INSTANCE = new BloodForensicsEngine();
    private static final TagKey<Item> BLOOD_TOOLS = TagKey.create(Registries.ITEM,
            ResourceLocation.parse("darkfolklore:investigation_tools/blood"));
    private static final double ANALYSIS_RADIUS_SQ = 8.0D * 8.0D;

    private BloodForensicsEngine() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onAnalyze(PlayerInteractEvent.RightClickBlock event) {
        if (!LivingFolkloreConfig.LIVING_FOLKLORE.get() || !LivingFolkloreConfig.BLOOD_FORENSICS.get()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !player.isShiftKeyDown() || !player.getMainHandItem().is(BLOOD_TOOLS)
                || !(player.level() instanceof ServerLevel level)) return;

        FolkloreSavedData data = FolkloreSavedData.get(player.getServer());
        ContractAssignment assignment = data.activeContract(player.getUUID()).orElse(null);
        if (assignment == null || (assignment.contract().status() != ContractStatus.INVESTIGATING
                && assignment.contract().status() != ContractStatus.IDENTIFIED)) return;
        var profile = FolkloreDataManager.INSTANCE.investigationProfile(assignment.contract().targetConcept()).orElse(null);
        if (profile == null || !profile.signatures().contains(EvidenceType.BLOOD_RESONANCE)) return;

        InvestigationSavedData investigation = InvestigationSavedData.get(player.getServer());
        InvestigationCaseLink link = investigation.caseLink(assignment.contract().id()).orElse(null);
        PersistentStory story = InvestigationTargeting.exactLinkedStory(data, link);
        long now = level.getGameTime();
        String dimension = level.dimension().location().toString();
        EvidenceRecord blood = data.evidence().stream()
                .filter(value -> value.type() == EvidenceType.BLOOD && !value.expired(now)
                        && value.position().dimension().equals(dimension)
                        && value.position().distanceSquared(event.getPos()) <= ANALYSIS_RADIUS_SQ
                        && InvestigationTargeting.matchesEvidence(assignment.contract().targetConcept(), value, link, story))
                .min(Comparator.comparingDouble(value -> value.position().distanceSquared(event.getPos())))
                .orElse(null);
        if (blood == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "No case-linked blood trace is close enough to compare."), true);
            return;
        }

        ContractEvidenceProgression.Result progression = ContractEvidenceProgression.record(
                player, data, assignment, EvidenceType.BLOOD_RESONANCE);
        if (!progression.changed()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "This blood resonance has already been recorded."), true);
            return;
        }
        LoreEngine.INSTANCE.grant(player, assignment.contract().targetConcept(), 5);
        LoreEngine.INSTANCE.grant(player, "darkfolklore:blood_magic", 3);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, blood.position().x() + 0.5D,
                blood.position().y() + 0.7D, blood.position().z() + 0.5D,
                12, 0.3D, 0.2D, 0.3D, 0.02D);
        CasebookService.INSTANCE.record(player, assignment, EvidenceType.BLOOD_RESONANCE,
                CaseNoteKind.ANALYSIS, "Blood Magic analysis found a supernatural blood resonance.",
                Optional.empty(), 0.8F, Optional.empty());
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                progression.identifiedNow()
                        ? "Blood forensics: the accumulated evidence now supports a conclusive identification."
                        : "Blood forensics: supernatural resonance recorded. It narrows the case, but does not identify the creature by itself."), false);
    }
}
