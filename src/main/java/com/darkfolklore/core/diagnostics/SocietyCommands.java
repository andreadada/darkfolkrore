package com.darkfolklore.core.diagnostics;

import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.society.village.VillageKey;
import com.darkfolklore.core.society.village.VillageResponseRules;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SocietyCommands {
    private SocietyCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("folklore")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("village")
                        .then(Commands.literal("response")
                                .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    String key = VillageKey.at(player.serverLevel(), player.blockPosition()).serialized();
                                    var state = FolkloreSavedData.get(context.getSource().getServer()).village(key);
                                    var response = VillageResponseRules.assess(state);
                                    send(context.getSource(), "Village=" + key + " response=" + response.tier()
                                            + " hunterReadiness=" + response.hunterReadiness()
                                            + " supernaturalPressure=" + response.supernaturalPressure()
                                            + " crisis=" + response.crisis());
                                    send(context.getSource(), response.message());
                                    return response.tier().ordinal();
                                })))));
    }

    private static void send(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }
}
