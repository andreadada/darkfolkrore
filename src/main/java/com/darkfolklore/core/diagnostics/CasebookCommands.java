package com.darkfolklore.core.diagnostics;

import com.darkfolklore.core.living.casebook.LivingFolkloreSavedData;
import com.darkfolklore.core.living.casebook.CasebookService;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class CasebookCommands {
    private CasebookCommands() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("folklore").then(Commands.literal("case")
                .executes(ctx->{var player=ctx.getSource().getPlayerOrException();var value=LivingFolkloreSavedData.get(player.getServer()).activeCase(player.getUUID()).orElse(null);if(value==null){send(ctx.getSource(),"No active Living Folklore case.");return 0;}send(ctx.getSource(),"Case "+value.id()+" stage="+value.stage()+" evidence="+value.evidence());var assignment=FolkloreSavedData.get(player.getServer()).contract(value.contractId()).orElse(null);if(assignment!=null&&value.identifiedConcept().isEmpty())send(ctx.getSource(),CasebookService.INSTANCE.summary(assignment));else value.identifiedConcept().ifPresent(x->send(ctx.getSource(),"Identified: "+x));return 1;})
                .then(Commands.literal("notes").executes(ctx->{var player=ctx.getSource().getPlayerOrException();var value=LivingFolkloreSavedData.get(player.getServer()).activeCase(player.getUUID()).orElse(null);if(value==null)return 0;value.notes().stream().skip(Math.max(0,value.notes().size()-12L)).forEach(n->send(ctx.getSource(),"["+n.kind()+"] "+n.detail()));return value.notes().size();}))));
    }
    private static void send(CommandSourceStack source,String text){source.sendSuccess(()->Component.literal(text),false);}
}
