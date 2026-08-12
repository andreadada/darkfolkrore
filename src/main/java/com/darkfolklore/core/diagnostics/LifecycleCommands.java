package com.darkfolklore.core.diagnostics;

import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.lifecycle.McaVampireLifecycleClassifier;
import com.darkfolklore.core.lifecycle.McaVampireLifecycleEngine;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public final class LifecycleCommands {
    private LifecycleCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> lifecycle = Commands.literal("lifecycle");
        lifecycle.then(Commands.literal("status").executes(context -> {
            var bridge = CompatibilityManager.INSTANCE.mcaVampireLifecycle();
            send(context.getSource(), "MCA vampire lifecycle bridge=" + bridge.runtimeAvailable()
                    + " tracked=" + McaVampireLifecycleEngine.INSTANCE.trackedEntities()
                    + " pendingBirths=" + McaVampireLifecycleEngine.INSTANCE.pendingBirths());
            CompatibilityManager.INSTANCE.report("mca_vamp_compat").ifPresent(report ->
                    send(context.getSource(), "MCA Vamp Compat actual=" + report.actualVersion()
                            + " status=" + report.status() + " detail=" + report.detail()));
            return bridge.runtimeAvailable() ? 1 : 0;
        }));
        lifecycle.then(Commands.literal("inspect")
                .then(Commands.argument("entity", EntityArgument.entity()).executes(context -> {
                    Entity entity = EntityArgument.getEntity(context, "entity");
                    var snapshot = CompatibilityManager.INSTANCE.mcaVampireLifecycle().snapshot(entity);
                    send(context.getSource(), "Lifecycle inspect " + entity.getName().getString()
                            + " uuid=" + entity.getUUID());
                    send(context.getSource(), "provider available=" + snapshot.available()
                            + " mca=" + snapshot.mcaVillager()
                            + " state=" + McaVampireLifecycleClassifier.state(snapshot)
                            + " infected=" + snapshot.infected()
                            + " converted=" + snapshot.converted()
                            + " curing=" + snapshot.curing()
                            + " inheritedProcessed=" + snapshot.inheritanceProcessed()
                            + " biteConversion=" + snapshot.biteWasConversionCause()
                            + " ai=" + snapshot.aiGoalsAdded()
                            + " source=" + snapshot.source().orElse(null));
                    McaVampireLifecycleEngine.INSTANCE.latest(entity.getUUID()).ifPresentOrElse(observation ->
                                    send(context.getSource(), "last transition=" + observation.transition()
                                            + " state=" + observation.state()
                                            + " source=" + observation.source().orElse(null)
                                            + " birth=" + observation.birth().orElse(null)
                                            + " t=" + observation.gameTime()),
                            () -> send(context.getSource(), "last transition: none observed in this runtime"));
                    return snapshot.available() ? 1 : 0;
                })));
        dispatcher.register(Commands.literal("folklore")
                .requires(source -> source.hasPermission(2))
                .then(lifecycle));
    }

    private static void send(CommandSourceStack source, String value) {
        source.sendSuccess(() -> Component.literal(value), false);
    }
}
