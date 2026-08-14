package com.darkfolklore.core.diagnostics;

import com.darkfolklore.core.knowledge.lore.KnowledgeDossier;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class KnowledgeCommands {
    private KnowledgeCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("folklore")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("knowledge")
                        .then(Commands.literal("dossier")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("concept", ResourceLocationArgument.id())
                                                .executes(context -> {
                                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                                    String concept = ResourceLocationArgument.getId(context, "concept").toString();
                                                    var progress = FolkloreSavedData.get(context.getSource().getServer())
                                                            .lore(player.getUUID(), concept);
                                                    KnowledgeDossier dossier = KnowledgeDossier.from(concept, progress);
                                                    send(context.getSource(), "Dossier " + concept + " player="
                                                            + player.getName().getString() + " points=" + progress.points()
                                                            + " stage=" + progress.stage());
                                                    send(context.getSource(), "revealed=" + dossier.revealed());
                                                    send(context.getSource(), "hidden=" + dossier.hidden());
                                                    return progress.points();
                                                }))))));
    }

    private static void send(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }
}
