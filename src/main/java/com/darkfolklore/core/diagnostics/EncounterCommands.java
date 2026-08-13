package com.darkfolklore.core.diagnostics;

import com.darkfolklore.core.compat.l2hostility.L2HostilityBridge;
import com.darkfolklore.core.encounter.*;
import com.darkfolklore.core.ward.WardSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.Locale;

public final class EncounterCommands {
    private EncounterCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("folklore").requires(source -> source.hasPermission(2))
                .then(Commands.literal("encounter")
                        .then(Commands.literal("list").executes(context -> list(context.getSource())))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(context -> inspect(context.getSource(), StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("seed")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .executes(context -> seed(context.getSource(), StringArgumentType.getString(context, "type")))))
                        .then(Commands.literal("l2").executes(context -> {
                            send(context.getSource(), "L2 Hostility=" + L2HostilityBridge.INSTANCE.diagnostics()); return 1;
                        })))
                .then(Commands.literal("ward")
                        .then(Commands.literal("list").executes(context -> wards(context.getSource())))));
    }

    private static int list(CommandSourceStack source) {
        var values = LegendaryEncounterSavedData.get(source.getServer()).encounters().stream()
                .sorted(Comparator.comparingLong(EncounterInstance::createdAt).reversed()).toList();
        if (values.isEmpty()) { send(source, "No legendary/story encounters are recorded."); return 0; }
        for (EncounterInstance value : values.stream().limit(32).toList()) {
            send(source, value.id() + " " + value.definitionId() + " stage=" + value.stage()
                    + " rank=" + value.rank() + " origin=" + value.origin() + " region=" + value.region()
                    + " entity=" + value.manifestationEntity().orElse(null));
        }
        return values.size();
    }

    private static int inspect(CommandSourceStack source, String raw) {
        try {
            var id = java.util.UUID.fromString(raw);
            EncounterInstance value = LegendaryEncounterSavedData.get(source.getServer()).encounter(id).orElse(null);
            if (value == null) { send(source, "Unknown encounter " + raw); return 0; }
            send(source, "Encounter " + value.id() + " definition=" + value.definitionId());
            send(source, "concept=" + value.concept() + " implementation=" + value.implementation()
                    + " rank=" + value.rank() + " spawnMode=" + value.spawnMode());
            send(source, "origin=" + value.origin() + " stage=" + value.stage() + " omens=" + value.omensCompleted());
            send(source, "anchor=" + value.anchor() + " region=" + value.region() + " entity=" + value.manifestationEntity().orElse(null)
                    + " story=" + value.storyId().orElse(null));
            send(source, "originPerson=" + value.originPerson().orElse(null) + " resolution=" + value.resolution());
            return 1;
        } catch (IllegalArgumentException ex) {
            send(source, "Encounter id must be a UUID."); return 0;
        }
    }

    private static int seed(CommandSourceStack source, String raw) {
        ServerPlayer player;
        try { player = source.getPlayerOrException(); } catch (Exception ex) { send(source, "Run seed as a player."); return 0; }
        EncounterDefinition def = switch (raw.toLowerCase(Locale.ROOT)) {
            case "wendigo" -> EncounterCatalog.WENDIGO;
            case "chupacabra" -> EncounterCatalog.CHUPACABRA;
            case "revenant" -> EncounterCatalog.REVENANT;
            case "wild_hunt", "wildhunt" -> EncounterCatalog.WILD_HUNT;
            default -> null;
        };
        if (def == null) { send(source, "Unknown type. Use wendigo|chupacabra|revenant|wild_hunt"); return 0; }
        EncounterOrigin origin = switch (def.id()) {
            case "darkfolklore:wendigo_hunger" -> EncounterOrigin.LOST_PERSON;
            case "darkfolklore:livestock_panic" -> EncounterOrigin.BLOOD_EVENT;
            case "darkfolklore:returned_dead" -> EncounterOrigin.VIOLENT_DEATH;
            case "darkfolklore:wild_hunt" -> EncounterOrigin.WORLD_OMEN;
            default -> def.origins().stream().findFirst().orElse(EncounterOrigin.WORLD_OMEN);
        };
        var created = LegendaryEncounterEngine.INSTANCE.createEncounter(LegendaryEncounterSavedData.get(source.getServer()),
                def, origin, player.serverLevel(), player.blockPosition(), player.serverLevel().getGameTime(), null);
        if (created.isEmpty()) { send(source, "Encounter seed rejected by provider/active-region/global-budget rules."); return 0; }
        send(source, "Seeded " + def.id() + " as " + created.get().id()); return 1;
    }

    private static int wards(CommandSourceStack source) {
        var wards = WardSavedData.get(source.getServer()).wards();
        if (wards.isEmpty()) { send(source, "No active/persisted wards."); return 0; }
        for (var ward : wards.stream().limit(32).toList()) {
            send(source, ward.id() + " " + ward.type() + " strength=" + ward.strength() + " radius=" + ward.radius()
                    + " anchor=" + ward.anchor() + " expires=" + ward.expiresAt());
        }
        return wards.size();
    }

    private static void send(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }
}
