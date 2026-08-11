package com.darkfolklore.core.investigation;

import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.contracts.ContractAssignment;
import com.darkfolklore.core.contracts.ContractStatus;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.knowledge.lore.KnowledgeStage;
import com.darkfolklore.core.knowledge.lore.LoreEngine;
import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.magic.MagicTradition;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.persistence.InvestigationSavedData;
import com.darkfolklore.core.reputation.ReputationFaction;
import com.darkfolklore.core.traits.ItemTrait;
import com.darkfolklore.core.traits.TraitResolver;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;

/**
 * Physical clues + five existing magic traditions + hypotheses + lore/Field
 * Guide progression + knowledge-gated preparation + explicit bounded tracking.
 */
public final class OccultInvestigationEngine {
    public static final OccultInvestigationEngine INSTANCE = new OccultInvestigationEngine();
    private final Map<UUID, Long> trackingCooldown = new HashMap<>();
    private final Map<UUID, UUID> announcedIdentification = new HashMap<>();

    private OccultInvestigationEngine() {}

    @SubscribeEvent
    public void onAnalyze(PlayerInteractEvent.RightClickBlock event) {
        if (!FolkloreConfig.OCCULT_INVESTIGATION.get()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !player.isShiftKeyDown()
                || player.getMainHandItem().isEmpty()
                || !(player.level() instanceof ServerLevel level)) return;

        FolkloreSavedData data = FolkloreSavedData.get(player.getServer());
        ContractAssignment assignment = data.activeContract(player.getUUID()).orElse(null);
        if (assignment == null || (assignment.contract().status() != ContractStatus.INVESTIGATING
                && assignment.contract().status() != ContractStatus.IDENTIFIED)) return;

        MagicTradition tradition = MagicPracticeResolver.resolve(player.getMainHandItem()).orElse(null);
        if (tradition == null) return;
        InvestigationProfile profile = FolkloreDataManager.INSTANCE
                .investigationProfile(assignment.contract().targetConcept()).orElse(null);
        if (profile == null) return;
        EvidenceType derived = derivedEvidence(profile, tradition, player.getMainHandItem());
        if (derived == null) {
            player.displayClientMessage(Component.literal(
                    "This magical tradition finds no decisive signature in this case."), true);
            return;
        }

        String dimension = level.dimension().location().toString();
        double radiusSq = Math.pow(FolkloreConfig.OCCULT_ANALYSIS_RADIUS.get(), 2);
        long now = level.getGameTime();
        EvidenceRecord clue = data.evidence().stream()
                .filter(value -> !value.expired(now)
                        && value.concept().equals(assignment.contract().targetConcept())
                        && value.position().dimension().equals(dimension)
                        && value.position().distanceSquared(event.getPos()) <= radiusSq)
                .min(Comparator.comparingDouble(value -> value.position().distanceSquared(event.getPos())))
                .orElse(null);
        if (clue == null) {
            player.displayClientMessage(Component.literal("No occult-readable trace is close enough to this point."), true);
            return;
        }

        boolean changed = assignment.contract().status() == ContractStatus.INVESTIGATING
                ? assignment.contract().addEvidence(derived, assignment.requiredDistinctClues())
                : assignment.contract().recordEvidence(derived);
        if (!changed) {
            player.displayClientMessage(Component.literal(
                    "You have already extracted this " + tradition.name().toLowerCase(Locale.ROOT) + " signature."), true);
            return;
        }

        data.putContract(assignment);
        LoreEngine.INSTANCE.grant(player, assignment.contract().targetConcept(), 8);
        LoreEngine.INSTANCE.grant(player, MagicPracticeResolver.knowledgeConcept(tradition), 3);
        sendAnalysisParticles(level, clue, tradition);
        player.displayClientMessage(Component.literal("Occult analysis [" + tradition + "]: " + derived
                + ". " + hypothesisSummary(assignment)), false);
        ensureObservedIfIdentified(player, data, assignment);
        // Analysis is additive: provider-owned right-click behavior remains untouched.
    }

    @SubscribeEvent
    public void onTrackingPulse(PlayerInteractEvent.RightClickItem event) {
        if (!FolkloreConfig.OCCULT_INVESTIGATION.get()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !player.isShiftKeyDown()
                || !(player.level() instanceof ServerLevel level)) return;
        FolkloreSavedData data = FolkloreSavedData.get(player.getServer());
        ContractAssignment assignment = data.activeContract(player.getUUID()).orElse(null);
        if (assignment == null || assignment.contract().status() != ContractStatus.IDENTIFIED) return;
        InvestigationProfile profile = FolkloreDataManager.INSTANCE
                .investigationProfile(assignment.contract().targetConcept()).orElse(null);
        if (profile == null || !isTrackingImplement(player.getMainHandItem(), profile)) return;

        long now = level.getGameTime();
        long readyAt = trackingCooldown.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        if (now < readyAt) {
            player.displayClientMessage(Component.literal("The trace is still settling."), true);
            return;
        }
        trackingCooldown.put(player.getUUID(), now + FolkloreConfig.TRACKING_COOLDOWN.get());

        int radius = Math.min(profile.trackingRadius(), FolkloreConfig.TRACKING_RADIUS.get());
        InvestigationSavedData investigation = InvestigationSavedData.get(player.getServer());
        InvestigationCaseLink link = investigation.caseLink(assignment.contract().id()).orElse(null);
        LivingEntity target = resolveTrackingTarget(level, player, assignment, link, radius);

        if (target == null) {
            String message = link != null && link.culpritId().isPresent() && !link.culpritFallbackAllowed()
                    ? "The incident culprit's trace is not present in the loaded search area."
                    : "No matching supernatural trace is present in the loaded search area.";
            player.displayClientMessage(Component.literal(message), true);
            return;
        }

        double distance = player.distanceTo(target);
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double dy = target.getY() - player.getY();
        String direction = cardinal(dx, dz);
        player.displayClientMessage(Component.literal("Tracking pulse: " + Math.round(distance)
                + "m " + direction + ", elevation " + signed(Math.round(dy)) + "."), false);
        drawTrace(level, player, target);
        // Never force-load chunks and never cancel the provider item's native use.
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!FolkloreConfig.OCCULT_INVESTIGATION.get()
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % 40 != 0) return;
        FolkloreSavedData data = FolkloreSavedData.get(player.getServer());
        ContractAssignment assignment = data.activeContract(player.getUUID()).orElse(null);
        if (assignment == null || assignment.contract().status() != ContractStatus.IDENTIFIED) {
            announcedIdentification.remove(player.getUUID());
            trackingCooldown.remove(player.getUUID());
            return;
        }
        ensureObservedIfIdentified(player, data, assignment);
        long now = player.serverLevel().getGameTime();
        if (trackingCooldown.size() > 1024) {
            trackingCooldown.entrySet().removeIf(entry -> entry.getValue() <= now);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPreparedHunt(LivingDeathEvent event) {
        if (!FolkloreConfig.OCCULT_INVESTIGATION.get() || !FolkloreConfig.PREPARED_HUNT_BONUS.get()
                || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        FolkloreSavedData data = FolkloreSavedData.get(player.getServer());
        ContractAssignment assignment = data.activeContract(player.getUUID()).orElse(null);
        if (assignment == null || assignment.contract().status() != ContractStatus.IDENTIFIED) return;
        InvestigationCaseLink link = InvestigationSavedData.get(player.getServer())
                .caseLink(assignment.contract().id()).orElse(null);
        if (!InvestigationTargeting.matches(assignment, event.getEntity(), link)) return;
        InvestigationProfile profile = FolkloreDataManager.INSTANCE
                .investigationProfile(assignment.contract().targetConcept()).orElse(null);
        if (profile == null) return;
        PreparationAssessment preparation = PreparationAssessment.evaluate(player, profile);
        if (!preparation.prepared()) return;
        LoreEngine.INSTANCE.grant(player, assignment.contract().targetConcept(), 5);
        data.addReputation(player.getUUID(), ReputationFaction.HUNTERS, 2);
        player.displayClientMessage(Component.literal(
                "Prepared hunt: your studied countermeasure earned bonus lore and hunter reputation."), true);
    }

    public List<String> status(ServerPlayer player) {
        FolkloreSavedData data = FolkloreSavedData.get(player.getServer());
        ContractAssignment assignment = data.activeContract(player.getUUID()).orElse(null);
        if (assignment == null) return List.of("No active Dark Folklore contract.");
        List<String> lines = new ArrayList<>();
        lines.add("Contract=" + assignment.contract().id() + " status=" + assignment.contract().status());
        lines.add("Evidence=" + assignment.contract().evidence());
        InvestigationCaseLink link = InvestigationSavedData.get(player.getServer())
                .caseLink(assignment.contract().id()).orElse(null);
        if (link != null) {
            lines.add("Incident story=" + link.storyId().orElse(null)
                    + " culprit=" + link.culpritId().orElse(null)
                    + " implementation=" + (link.observedImplementation().isBlank() ? "-" : link.observedImplementation())
                    + " culpritFallback=" + link.culpritFallbackAllowed()
                    + " issuerFallback=" + link.issuerFallbackAllowed());
        }
        if (assignment.contract().status() == ContractStatus.INVESTIGATING) lines.add(hypothesisSummary(assignment));
        FolkloreDataManager.INSTANCE.investigationProfile(assignment.contract().targetConcept()).ifPresent(profile -> {
            if (assignment.contract().status() == ContractStatus.IDENTIFIED
                    || assignment.contract().status() == ContractStatus.HUNTED) {
                PreparationAssessment p = PreparationAssessment.evaluate(player, profile);
                lines.add("Preparation knowledge=" + p.knowledgeStage() + " known=" + p.hasKnownCountermeasure()
                        + " prepared=" + p.prepared() + " satisfied=" + p.satisfiedRules()
                        + " missingOptions=" + p.missingOptions());
                lines.add("Tracking traditions=" + profile.analysisResults().keySet()
                        + " radius=" + Math.min(profile.trackingRadius(), FolkloreConfig.TRACKING_RADIUS.get()));
            }
        });
        return List.copyOf(lines);
    }

    public List<Hypothesis> hypotheses(ServerPlayer player) {
        return FolkloreSavedData.get(player.getServer()).activeContract(player.getUUID())
                .map(value -> HypothesisEngine.rank(value.contract().evidence())).orElse(List.of());
    }

    public Optional<InvestigationProfile> profile(String concept) {
        return FolkloreDataManager.INSTANCE.investigationProfile(concept);
    }

    private void ensureObservedIfIdentified(ServerPlayer player, FolkloreSavedData data, ContractAssignment assignment) {
        if (assignment.contract().status() != ContractStatus.IDENTIFIED) return;
        int points = data.lore(player.getUUID(), assignment.contract().targetConcept()).points();
        if (points < KnowledgeStage.OBSERVED.threshold()) {
            LoreEngine.INSTANCE.grant(player, assignment.contract().targetConcept(),
                    KnowledgeStage.OBSERVED.threshold() - points);
        }
        UUID previous = announcedIdentification.put(player.getUUID(), assignment.contract().id());
        if (assignment.contract().id().equals(previous)) return;

        InvestigationCaseLink link = InvestigationSavedData.get(player.getServer())
                .caseLink(assignment.contract().id()).orElse(null);
        String implementation = link == null ? "" : link.observedImplementation();
        if (implementation.isBlank()) {
            implementation = FolkloreDataManager.INSTANCE.canonical().concept(assignment.contract().targetConcept())
                    .map(value -> value.canonicalId()).orElse("");
        }
        boolean guideSynced = !implementation.isBlank()
                && CompatibilityManager.INSTANCE.unlockFieldGuideImplementation(player, implementation);

        InvestigationProfile profile = FolkloreDataManager.INSTANCE
                .investigationProfile(assignment.contract().targetConcept()).orElse(null);
        PreparationAssessment preparation = profile == null ? null : PreparationAssessment.evaluate(player, profile);
        String preparationMessage;
        if (preparation == null) {
            preparationMessage = "";
        } else if (preparation.knowledgeStage().ordinal() < KnowledgeStage.STUDIED.ordinal()) {
            preparationMessage = " Weakness details remain hidden until this lore is STUDIED.";
        } else if (!preparation.hasKnownCountermeasure()) {
            preparationMessage = " No Core cross-mod countermeasure is currently documented.";
        } else if (preparation.prepared()) {
            preparationMessage = " You already carry a studied countermeasure.";
        } else {
            preparationMessage = " A studied countermeasure is known but not currently carried.";
        }
        player.displayClientMessage(Component.literal("Target identified: " + assignment.contract().targetConcept()
                + ". Lore advanced to OBSERVED."
                + (guideSynced ? " The observed Field Guide entry was synchronized." : "")
                + preparationMessage), false);
    }

    private static EvidenceType derivedEvidence(InvestigationProfile profile, MagicTradition tradition,
                                                ItemStack implement) {
        Set<ItemTrait> traits = TraitResolver.itemTraits(implement);
        if (tradition == MagicTradition.WITCHCRAFT) {
            if (profile.signatures().contains(EvidenceType.GARLIC_REACTION) && traits.contains(ItemTrait.GARLIC)) {
                return EvidenceType.GARLIC_REACTION;
            }
            if (profile.signatures().contains(EvidenceType.WOLFSBANE_REACTION)
                    && traits.contains(ItemTrait.WOLFSBANE)) {
                return EvidenceType.WOLFSBANE_REACTION;
            }
        }
        return profile.analysisResults().get(tradition);
    }

    private static boolean isTrackingImplement(ItemStack stack, InvestigationProfile profile) {
        Optional<MagicTradition> tradition = MagicPracticeResolver.resolve(stack);
        if (tradition.isPresent() && profile.analysisResults().containsKey(tradition.get())) return true;
        Set<ItemTrait> traits = TraitResolver.itemTraits(stack);
        return traits.contains(ItemTrait.MONSTER_PART)
                || traits.contains(ItemTrait.SPIRITUAL)
                || traits.contains(ItemTrait.SOUL)
                || traits.contains(ItemTrait.FAE);
    }

    public String hypothesisSummary(ContractAssignment assignment) {
        List<Hypothesis> values = HypothesisEngine.rank(assignment.contract().evidence());
        if (values.isEmpty()) return "Hypotheses: insufficient evidence.";
        return "Hypotheses: " + values.stream().limit(3)
                .map(value -> value.concept() + " support=" + Math.round(value.confidence() * 100.0F) + "%")
                .toList();
    }

    private static LivingEntity resolveTrackingTarget(ServerLevel level, ServerPlayer player,
                                                      ContractAssignment assignment, InvestigationCaseLink link,
                                                      int radius) {
        if (link != null && link.culpritId().isPresent() && !link.culpritFallbackAllowed()) {
            Entity exact = level.getEntity(link.culpritId().get());
            if (exact instanceof LivingEntity living && living.isAlive() && living != player
                    && player.distanceToSqr(living) <= (double) radius * radius
                    && InvestigationTargeting.matches(assignment, living, link)) return living;
            return null;
        }
        return level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                        entity -> entity.isAlive() && entity != player
                                && InvestigationTargeting.matches(assignment, entity, link))
                .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    private static void sendAnalysisParticles(ServerLevel level, EvidenceRecord clue, MagicTradition tradition) {
        ParticleOptions particle = switch (tradition) {
            case WITCHCRAFT -> ParticleTypes.COMPOSTER;
            case SPIRIT -> ParticleTypes.ENCHANT;
            case SOUL -> ParticleTypes.SOUL;
            case FORBIDDEN_THEURGY -> ParticleTypes.WITCH;
            case FAE -> ParticleTypes.END_ROD;
        };
        level.sendParticles(particle, clue.position().x() + 0.5D, clue.position().y() + 0.8D,
                clue.position().z() + 0.5D, 14, 0.35D, 0.25D, 0.35D, 0.02D);
    }

    private static void drawTrace(ServerLevel level, ServerPlayer player, LivingEntity target) {
        double dx = target.getX() - player.getX();
        double dy = target.getY() + target.getBbHeight() * 0.5D - (player.getY() + 1.0D);
        double dz = target.getZ() - player.getZ();
        for (int i = 1; i <= 10; i++) {
            double t = i / 11.0D;
            level.sendParticles(ParticleTypes.ENCHANT,
                    player.getX() + dx * t, player.getY() + 1.0D + dy * t, player.getZ() + dz * t,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static String cardinal(double dx, double dz) {
        double ax = Math.abs(dx);
        double az = Math.abs(dz);
        if (ax >= az * 2.0D) return dx >= 0 ? "E" : "W";
        if (az >= ax * 2.0D) return dz >= 0 ? "S" : "N";
        return (dz < 0 ? "N" : "S") + (dx < 0 ? "W" : "E");
    }

    private static String signed(long value) {
        return value >= 0 ? "+" + value : Long.toString(value);
    }
}
