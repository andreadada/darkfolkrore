package com.darkfolklore.core.diagnostics;

import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.compat.CompatibilityStatus;
import com.darkfolklore.core.compat.mca.McaVampCompatAdapter;
import com.darkfolklore.core.predation.PredationTraceEngine;
import com.darkfolklore.core.predation.PredatorKind;
import com.darkfolklore.core.predation.VampirePredationEngine;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.fml.ModList;

public final class PredationCommands {
    private PredationCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> predation = Commands.literal("predation");
        predation.then(Commands.literal("status").executes(context -> {
            var bridge = CompatibilityManager.INSTANCE.vampirePredation();
            send(context.getSource(), "Dark Folklore runtime=" + modVersion("darkfolklore")
                    + " source=" + codeSourceName(CompatibilityManager.class));
            send(context.getSource(), "Vampire predation bridge=" + bridge.runtimeAvailable()
                    + " wild=" + bridge.wildRuntimeAvailable()
                    + " mca=" + bridge.mcaRuntimeAvailable()
                    + " activeSessions=" + VampirePredationEngine.INSTANCE.activeSessions()
                    + " pendingLethal=" + VampirePredationEngine.INSTANCE.pendingLethalIntents()
                    + " trackedRegions=" + VampirePredationEngine.INSTANCE.trackedRegions()
                    + " traces=" + PredationTraceEngine.INSTANCE.tracked());
            send(context.getSource(), "predation detail=" + bridge.runtimeDetail());
            if (!bridge.circuitStatus().isEmpty()) send(context.getSource(), "circuits=" + bridge.circuitStatus());
            CompatibilityManager.INSTANCE.report(McaVampCompatAdapter.MOD_ID).ifPresent(report -> {
                String normalized = McaVampCompatAdapter.normalizeVersion(report.actualVersion());
                boolean exactKnown = McaVampCompatAdapter.supportsVersion(report.actualVersion());
                boolean probeEligible = McaVampCompatAdapter.runtimeProbeEligible(report.actualVersion());
                send(context.getSource(), "MCA Vamp Compat actual=" + report.actualVersion()
                        + " normalized=" + normalized
                        + " known=" + exactKnown
                        + " probeEligible=" + probeEligible
                        + " status=" + report.status() + " detail=" + report.detail());
                if (report.status() == CompatibilityStatus.UNTESTED_VERSION && probeEligible) {
                    send(context.getSource(), "COMPAT INVARIANT ERROR: runtime gate accepts this version but cached report is UNTESTED; check duplicate/stale Dark Folklore classes");
                }
            });
            return bridge.runtimeAvailable() ? 1 : 0;
        }));
        predation.then(Commands.literal("inspect")
                .then(Commands.argument("entity", EntityArgument.entity()).executes(context -> {
                    Entity entity = EntityArgument.getEntity(context, "entity");
                    var bridge = CompatibilityManager.INSTANCE.vampirePredation();
                    var provider = bridge.providerSnapshot(entity);
                    PredatorKind kind = entity instanceof Mob mob ? bridge.predatorKind(mob) : PredatorKind.NONE;
                    send(context.getSource(), "Predation inspect " + entity.getName().getString()
                            + " uuid=" + entity.getUUID() + " kind=" + kind
                            + " core=" + modVersion("darkfolklore"));
                    send(context.getSource(), "provider available=" + provider.available()
                            + " mca=" + provider.mcaVillager() + " vampire=" + provider.vampire()
                            + " infected=" + provider.infected() + " converted=" + provider.converted()
                            + " curing=" + provider.curing() + " recentBite=" + provider.recentBite()
                            + " nativeAi=" + provider.aiGoalsAdded() + " detail=" + provider.detail());
                    send(context.getSource(), "bridge wild=" + bridge.wildRuntimeAvailable()
                            + " mca=" + bridge.mcaRuntimeAvailable() + " detail=" + bridge.runtimeDetail());
                    if (!bridge.circuitStatus().isEmpty()) send(context.getSource(), "circuits=" + bridge.circuitStatus());
                    VampirePredationEngine.INSTANCE.diagnostic(entity.getUUID()).ifPresentOrElse(value -> {
                                send(context.getSource(), "behavior profile=" + value.behaviorProfile()
                                        + " intent=" + value.intent() + " source=" + value.profileDetail());
                                send(context.getSource(), "director localRisk=" + Math.round(value.localRisk())
                                        + " personalRisk=" + Math.round(value.personalRisk())
                                        + " target=" + value.target().orElse(null)
                                        + " phase=" + VampirePredationEngine.INSTANCE.sessionPhase(entity.getUUID()).orElse(null)
                                        + " reason=" + value.reason() + " t=" + value.gameTime());
                            },
                            () -> send(context.getSource(), "director: no recent decision for this entity"));
                    return 1;
                })));
        predation.then(Commands.literal("trace")
                .then(Commands.argument("entity", EntityArgument.entity()).executes(context -> {
                    Entity entity = EntityArgument.getEntity(context, "entity");
                    var trace = PredationTraceEngine.INSTANCE.trace(entity.getUUID()).orElse(null);
                    if (trace == null) {
                        send(context.getSource(), "No predation trace for " + entity.getName().getString()
                                + ". Keep the entity loaded for at least one predation scan interval.");
                        return 0;
                    }
                    send(context.getSource(), "Predation trace " + entity.getName().getString()
                            + " kind=" + trace.kind() + " profile=" + trace.behaviorProfile()
                            + " intent=" + trace.intent() + " phase=" + trace.phase()
                            + " wantsBlood=" + trace.wantsBlood());
                    send(context.getSource(), "profileSource=" + trace.profileDetail());
                    send(context.getSource(), "environment day=" + trace.day() + " skyVisible=" + trace.skyVisible()
                            + " allowed=" + trace.environmentAllowed() + " localRisk=" + Math.round(trace.localRisk())
                            + " personalRisk=" + Math.round(trace.personalRisk()));
                    send(context.getSource(), "selectedTarget=" + trace.selectedTarget().orElse(null)
                            + " detail=" + trace.detail() + " t=" + trace.gameTime());
                    for (var candidate : trace.candidates()) {
                        send(context.getSource(), "candidate " + candidate.name() + " " + candidate.entity()
                                + " animal=" + candidate.animal() + " mca=" + candidate.mcaCivilian()
                                + " provider=" + candidate.providerEligible()
                                + " knowsIdentity=" + candidate.victimKnowsIdentity()
                                + " witnesses=" + candidate.witnesses()
                                + " distance=" + Math.round(candidate.distance())
                                + " behaviorAdj=" + Math.round(candidate.behaviorAdjustment())
                                + " intent=" + candidate.predictedIntent()
                                + " eligible=" + candidate.eligible()
                                + " score=" + (Double.isFinite(candidate.score()) ? Math.round(candidate.score()) : "-")
                                + " reason=" + candidate.reason());
                    }
                    return trace.candidates().size();
                })));

        dispatcher.register(Commands.literal("folklore")
                .requires(source -> source.hasPermission(2))
                .then(predation));
    }

    private static String modVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("-");
    }

    private static String codeSourceName(Class<?> type) {
        try {
            var source = type.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) return "unknown";
            String value = source.getLocation().toExternalForm();
            while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
            int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
            return slash >= 0 ? value.substring(slash + 1) : value;
        } catch (RuntimeException exception) {
            return "unknown:" + exception.getClass().getSimpleName();
        }
    }

    private static void send(CommandSourceStack source, String value) {
        source.sendSuccess(() -> Component.literal(value), false);
    }
}
