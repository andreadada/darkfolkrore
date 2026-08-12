package com.darkfolklore.core.society.bloodline;

import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.knowledge.social.SecretType;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.society.SecretFacts;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class LineageEngine {
    public static final LineageEngine INSTANCE = new LineageEngine();
    private LineageEngine() {}

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Entity entity = event.getEntity();
        for (SecretType type : new SecretType[]{SecretType.VAMPIRE, SecretType.WEREWOLF}) {
            if (!SecretFacts.actualSecrets(entity).contains(type)) continue;
            CompatibilityManager.INSTANCE.conversionSource(entity, type)
                    .filter(source -> !source.equals(entity.getUUID()))
                    .ifPresent(source -> FolkloreSavedData.get(level.getServer()).addLineage(
                            new LineageRecord(entity.getUUID(), source, type, level.getGameTime())));
        }
    }
}
