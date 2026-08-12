package com.darkfolklore.core.diagnostics;

import com.darkfolklore.core.compat.CompatibilityManager;
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

public final class PredationCommands {
    private PredationCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> predation = Commands.literal("predation");
        predation.then(Commands.literal("status").executes(context -> {
            var bridge = CompatibilityManager.INSTANCE.vampirePredation();
            send(context.getSource(), "Vampire predation bridge=" + bridge.runtimeAvailable()
                    + " activeSessions=" + VampirePredationEngine.INSTANCE.activeSessions()
                    + " pendingLethal=" + VampirePredationEngine.INSTANCE.pendingLethalIntents()
                    + " trackedRegions=" + VampirePredationEngine.INSTANCE.trackedRegions()
                    + " traces=" + PredationTraceEngine.INSTANCE.tracked());
            if (!bridge.circuitStatus().isEmpty()) send(context.getSource(), "circuits=" + bridge.circuitStatus());
            CompatibilityManager.INSTANCE.report("mca_vamp_compat").ifPresent(report ->
                    send(context.getSource(), "MCA Vamp Compat actual=" + report.actualVersion()
                            + " status=" + report.status() + " detail=" + report.detail()));
            return bridge.runtimeAvailable() ? 1 : 0;
        }));
        predation.then(Commands.literal("inspect")
                .then(Commands.argument("entity", EntityArgument.entity()).executes(context -> {
                    Entity entity = EntityArgument.getEntity(context, "entity");
                    var bridge = CompatibilityManager.INSTANCE.vampirePredation();
                    var provider = bridge.providerSnapshot(entity);
                    PredatorKind kind = entity instanceof Mob mob ? bridge.predatorKind(mob) : PredatorKind.NONE;
                    send(context.getSource(), "Predation inspect " + entity.getName().getString()
                            + " uuid=" + entity.getUUID() + " kind=" + kind);
                    send(context.getSource(), "provider available=" + provider.available()
                            + " mca=" + provider.mcaVillager() + " vampire=" + provider.vampire()
                            + " infected=" + provider.infected() + " converted=" + provider.converted()
                            + " curing=" + provider.curing() + " recentBite=" + provider.recentBite()
                            + " nativeAi=" + provider.aiGoalsAdded() + " detail=" + provider.detail());
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

    private static void send(CommandSourceStack source, String value) {
        source.sendSuccess(() -> Component.literal(value), false);
    }
}
