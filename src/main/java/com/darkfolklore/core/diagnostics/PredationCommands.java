package com.darkfolklore.core.diagnostics;

import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.predation.PredatorKind;
import com.darkfolklore.core.predation.VampirePredationEngine;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public final class PredationCommands {
    private PredationCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("folklore")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("predation")
                        .then(Commands.literal("status").executes(context -> {
                            var bridge = CompatibilityManager.INSTANCE.vampirePredation();
                            send(context.getSource(), "Vampire predation bridge=" + bridge.runtimeAvailable()
                                    + " activeSessions=" + VampirePredationEngine.INSTANCE.activeSessions()
                                    + " trackedRegions=" + VampirePredationEngine.INSTANCE.trackedRegions());
                            CompatibilityManager.INSTANCE.report("mca_vamp_compat").ifPresent(report ->
                                    send(context.getSource(), "MCA Vamp Compat actual=" + report.actualVersion()
                                            + " status=" + report.status() + " detail=" + report.detail()));
                            return bridge.runtimeAvailable() ? 1 : 0;
                        }))
                        .then(Commands.literal("inspect")
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
                                    VampirePredationEngine.INSTANCE.diagnostic(entity.getUUID()).ifPresentOrElse(value ->
                                                    send(context.getSource(), "director localRisk=" + Math.round(value.localRisk())
                                                            + " personalRisk=" + Math.round(value.personalRisk())
                                                            + " target=" + value.target().orElse(null)
                                                            + " reason=" + value.reason() + " t=" + value.gameTime()),
                                            () -> send(context.getSource(), "director: no recent decision for this entity"));
                                    return 1;
                                }))));
    }

    private static void send(CommandSourceStack source, String value) {
        source.sendSuccess(() -> Component.literal(value), false);
    }
}
