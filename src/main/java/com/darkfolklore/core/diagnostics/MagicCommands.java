package com.darkfolklore.core.diagnostics;

import com.darkfolklore.core.magic.MagicDisciplineRegistry;
import com.darkfolklore.core.magic.MagicDisciplineResolver;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class MagicCommands {
    private MagicCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("folklore")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("magic")
                        .then(Commands.literal("disciplines").executes(context -> {
                            for (var discipline : MagicDisciplineRegistry.disciplines().stream().sorted().toList()) {
                                var profile = MagicDisciplineRegistry.profile(discipline);
                                send(context.getSource(), discipline + " concept=" + profile.knowledgeConcept()
                                        + " uses=" + profile.uses() + " providers=" + profile.providerNamespaces());
                            }
                            return MagicDisciplineRegistry.disciplines().size();
                        }))
                        .then(Commands.literal("inspect-held").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            var stack = player.getMainHandItem();
                            var disciplines = MagicDisciplineResolver.resolveAll(stack);
                            send(context.getSource(), "Held=" + stack.getHoverName().getString()
                                    + " disciplines=" + disciplines);
                            for (var discipline : disciplines) {
                                var profile = MagicDisciplineRegistry.profile(discipline);
                                send(context.getSource(), discipline + " uses=" + profile.uses());
                            }
                            return disciplines.size();
                        }))));
    }

    private static void send(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }
}
