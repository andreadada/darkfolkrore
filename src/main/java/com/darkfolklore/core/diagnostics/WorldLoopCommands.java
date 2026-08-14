package com.darkfolklore.core.diagnostics;

import com.darkfolklore.core.investigation.OccultInvestigationEngine;
import com.darkfolklore.core.magic.MagicDisciplineResolver;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.society.village.VillageKey;
import com.darkfolklore.core.society.village.VillageResponseRules;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class WorldLoopCommands {
    private WorldLoopCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("folklore")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("world")
                        .then(Commands.literal("status")
                                .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    var data = FolkloreSavedData.get(context.getSource().getServer());
                                    String villageKey = VillageKey.at(player.serverLevel(), player.blockPosition()).serialized();
                                    var response = VillageResponseRules.assess(data.village(villageKey));
                                    send(context.getSource(), "World loop for " + player.getName().getString());
                                    send(context.getSource(), "Village=" + villageKey + " tier=" + response.tier()
                                            + " hunterReadiness=" + response.hunterReadiness()
                                            + " pressure=" + response.supernaturalPressure()
                                            + " crisis=" + response.crisis());
                                    var active = data.activeContract(player.getUUID());
                                    if (active.isEmpty()) {
                                        send(context.getSource(), "Investigation: no active contract");
                                    } else {
                                        send(context.getSource(), "Investigation concept="
                                                + active.get().contract().targetConcept() + " status="
                                                + active.get().contract().status() + " evidence="
                                                + active.get().contract().evidence().size());
                                        var lore = data.lore(player.getUUID(), active.get().contract().targetConcept());
                                        send(context.getSource(), "Knowledge=" + lore.points() + " " + lore.stage());
                                        for (String line : OccultInvestigationEngine.INSTANCE.status(player)) {
                                            send(context.getSource(), "  " + line);
                                        }
                                    }
                                    var disciplines = MagicDisciplineResolver.resolveAll(player.getMainHandItem());
                                    send(context.getSource(), "Held disciplines=" + disciplines);
                                    return active.isPresent() ? 1 : 0;
                                })))));
    }

    private static void send(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }
}
