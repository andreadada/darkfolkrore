package com.darkfolklore.core.endgame;

import com.darkfolklore.core.api.event.ConfirmedLivingDeathEvent;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.persistence.WorldPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.Optional;
import java.util.Set;

public final class ForbiddenEndgameEngine {
    public static final ForbiddenEndgameEngine INSTANCE = new ForbiddenEndgameEngine();
    private static final ResourceLocation PHASE1 = ResourceLocation.parse("the_day_of_the_beast:devil_boss_phase_one");
    private static final ResourceLocation PHASE2 = ResourceLocation.parse("the_day_of_the_beast:devil_boss_phase_two");
    private static final ResourceLocation BEAST = ResourceLocation.parse("the_day_of_the_beast:beast");
    private static final ResourceLocation AZAZEL = ResourceLocation.parse("netherman:azazel");
    private static final ResourceLocation TRUE_AZAZEL = ResourceLocation.parse("netherman:azazel_human");
    private static final Set<ResourceLocation> PHASES = Set.of(PHASE1, PHASE2, BEAST);
    private ForbiddenEndgameEngine() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onFrame(PlayerInteractEvent.RightClickBlock event) {
        if (!EndgameConfig.FORBIDDEN_ENDGAME.get() || !EndgameConfig.DAY_OF_BEAST.get() || !ModList.get().isLoaded("the_day_of_the_beast")
                || event.getHand()!=InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level) || !DemonInvocationMultiblock.isFrame(level.getBlockState(event.getPos()))) return;
        event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        ItemStack heart = player.getMainHandItem();
        if (BeastHeartService.isBeastHeart(heart)) { message(player, "The Beast Heart cannot reopen the gate."); return; }
        if (!BeastHeartService.isNormalDemonHeart(heart)) { message(player, "The invocation frame remains dormant."); return; }
        if (EndgameConfig.REQUIRE_WITCHING_HOUR.get() && !isWitchingHour(level.getDayTime())) { message(player, "The frame answers only during the witching hour."); return; }
        if (!DemonInvocationMultiblock.validate(level,event.getPos()).valid()) { message(player, "The invocation monument is incomplete."); return; }
        ForbiddenEndgameSavedData data = ForbiddenEndgameSavedData.get(player.getServer());
        if (!data.hasCapacityForSite() || data.activeNear(level.dimension().location().toString(),event.getPos(),24).isPresent()) { message(player, "This place cannot sustain another invocation."); return; }
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(PHASE1);
        if (type.isEmpty()) { message(player, "The provider entity is unavailable; the heart was preserved."); return; }
        Entity entity = type.get().create(level);
        if (entity == null) return;
        BlockPos pos=event.getPos(); entity.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,player.getYRot(),0);
        if (!level.noCollision(entity) || !level.addFreshEntity(entity)) { entity.discard(); message(player,"The gate cannot form here; the heart was preserved."); return; }
        if (data.begin(player.getUUID(), WorldPosition.of(level,pos),level.getGameTime(),entity.getUUID()).isEmpty()) { entity.discard(); return; }
        if (!player.getAbilities().instabuild) heart.shrink(1);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,32,1.5,1,1.5,.05);
        player.displayClientMessage(Component.literal("The monument opens. Something answers."),false);
    }

    @SubscribeEvent public void onJoin(EntityJoinLevelEvent event) {
        if (!EndgameConfig.FORBIDDEN_ENDGAME.get() || !EndgameConfig.DAY_OF_BEAST.get() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!PHASES.contains(BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()))) return;
        ForbiddenEndgameSavedData data=ForbiddenEndgameSavedData.get(level.getServer());
        data.activeNear(level.dimension().location().toString(),event.getEntity().blockPosition(),96).ifPresent(s->data.bindParticipant(s,event.getEntity().getUUID()));
    }

    @SubscribeEvent public void onFinalDeath(ConfirmedLivingDeathEvent event) {
        if (!EndgameConfig.FORBIDDEN_ENDGAME.get()) return;
        ResourceLocation id=BuiltInRegistries.ENTITY_TYPE.getKey(event.entity().getType());
        ServerPlayer player=event.source().getEntity() instanceof ServerPlayer p?p:null;
        ForbiddenEndgameSavedData data=ForbiddenEndgameSavedData.get(event.server());
        if (EndgameConfig.DAY_OF_BEAST.get() && BEAST.equals(id)) data.byParticipant(event.entity().getUUID()).ifPresent(site->{
            if (site.state()!=DemonInvocationState.ACTIVE) return;
            data.complete(site,event.entity().level().getGameTime()); data.markMilestone(site.owner(),EndgameMilestone.DEVIL_SLAIN);
            BeastHeartService.create(event.entity().getUUID(),player!=null?player.getUUID():site.owner(),event.entity().level().getGameTime()).ifPresent(stack->deliver(event.entity(),player,stack));
            if (player!=null) data.markMilestone(player.getUUID(),EndgameMilestone.BEAST_HEART_CLAIMED);
        });
        if (player!=null && EndgameConfig.CULT_OF_AZAZEL.get()) {
            if (AZAZEL.equals(id)) data.markMilestone(player.getUUID(),EndgameMilestone.AZAZEL_SLAIN);
            if (TRUE_AZAZEL.equals(id)) data.markMilestone(player.getUUID(),EndgameMilestone.TRUE_AZAZEL_SLAIN);
        }
    }

    @SubscribeEvent public void onTick(ServerTickEvent.Post event) {
        if (EndgameConfig.FORBIDDEN_ENDGAME.get() && event.getServer().getTickCount()%1200==0)
            ForbiddenEndgameSavedData.get(event.getServer()).prune(event.getServer().overworld().getGameTime(), FolkloreConfig.HISTORY_RETENTION.get());
    }

    public static boolean isWitchingHour(long dayTime) { long t=Math.floorMod(dayTime,24000L); return t>=17500L && t<=18500L; }
    private static void message(ServerPlayer player,String text){player.displayClientMessage(Component.literal(text),true);}
    private static void deliver(Entity entity,ServerPlayer player,ItemStack stack){if(player!=null){if(!player.getInventory().add(stack))player.drop(stack,false);}else entity.spawnAtLocation(stack);}
}
