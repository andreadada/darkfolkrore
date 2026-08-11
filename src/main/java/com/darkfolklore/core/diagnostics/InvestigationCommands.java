package com.darkfolklore.core.diagnostics;

import com.darkfolklore.core.investigation.Hypothesis;
import com.darkfolklore.core.investigation.InvestigationProfile;
import com.darkfolklore.core.investigation.OccultInvestigationEngine;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class InvestigationCommands {
    public static final InvestigationCommands INSTANCE = new InvestigationCommands();
    private InvestigationCommands() {}

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> investigation = Commands.literal("investigation")
                .then(Commands.literal("status")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    OccultInvestigationEngine.INSTANCE.status(player)
                                            .forEach(line -> send(context.getSource(), line));
                                    return 1;
                                })))
                .then(Commands.literal("hypotheses")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    var values = OccultInvestigationEngine.INSTANCE.hypotheses(player);
                                    if (values.isEmpty()) send(context.getSource(), "No active hypotheses.");
                                    for (Hypothesis value : values) {
                                        send(context.getSource(), value.concept() + " score=" + value.score()
                                                + " matched=" + value.matchedEvidence() + "/" + value.observedEvidence()
                                                + " support=" + Math.round(value.confidence() * 100.0F) + "%");
                                    }
                                    return values.size();
                                })))
                .then(Commands.literal("profile")
                        .then(Commands.argument("concept", ResourceLocationArgument.id())
                                .executes(context -> {
                                    String concept = ResourceLocationArgument.getId(context, "concept").toString();
                                    InvestigationProfile profile = OccultInvestigationEngine.INSTANCE.profile(concept)
                                            .orElse(null);
                                    if (profile == null) {
                                        send(context.getSource(), "No investigation profile for " + concept);
                                        return 0;
                                    }
                                    send(context.getSource(), profile.concept()
                                            + " creatureTraits=" + profile.creatureTraits()
                                            + " signatures=" + profile.signatures()
                                            + " analyses=" + profile.analysisResults()
                                            + " trackingRadius=" + profile.trackingRadius());
                                    return 1;
                                })));

        event.getDispatcher().register(Commands.literal("folklore")
                .requires(source -> source.hasPermission(2))
                .then(investigation));
    }

    private static void send(CommandSourceStack source, String value) {
        source.sendSuccess(() -> Component.literal(value), false);
    }
}
