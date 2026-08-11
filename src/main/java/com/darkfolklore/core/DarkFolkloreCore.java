package com.darkfolklore.core;

import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.compat.wolfsbane.WolfsbaneIntegration;
import com.darkfolklore.core.canonical.FolkloreLootModifiers;
import com.darkfolklore.core.canonical.FolkloreBiomeModifiers;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.contracts.ContractEngine;
import com.darkfolklore.core.diagnostics.CoreServerEvents;
import com.darkfolklore.core.diagnostics.InvestigationCommands;
import com.darkfolklore.core.investigation.OccultInvestigationEngine;
import com.darkfolklore.core.knowledge.lore.LoreEngine;
import com.darkfolklore.core.society.bloodline.LineageEngine;
import com.darkfolklore.core.society.organization.OrganizationEngine;
import com.darkfolklore.core.society.rumor.RumorEngine;
import com.darkfolklore.core.society.story.IncidentStoryEngine;
import com.darkfolklore.core.society.story.SocietyStoryEngine;
import com.darkfolklore.core.society.witness.WitnessEngine;
import com.darkfolklore.core.spawn.SpawnDirector;
import com.darkfolklore.core.weakness.WeaknessEngine;
import com.darkfolklore.core.world.WorldEventDirector;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(DarkFolkloreCore.MOD_ID)
public final class DarkFolkloreCore {
    public static final String MOD_ID = "darkfolklore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DarkFolkloreCore(IEventBus modBus, ModContainer container) {
        LOGGER.info("[core] Dark Folklore Core loading");
        container.registerConfig(ModConfig.Type.COMMON, FolkloreConfig.SPEC);
        FolkloreBiomeModifiers.register(modBus);
        FolkloreLootModifiers.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(WolfsbaneIntegration::onCommonSetup);

        var bus = NeoForge.EVENT_BUS;
        bus.register(CoreServerEvents.INSTANCE);
        bus.register(WeaknessEngine.INSTANCE);
        bus.register(LoreEngine.INSTANCE);
        bus.register(WitnessEngine.INSTANCE);
        bus.register(RumorEngine.INSTANCE);
        bus.register(LineageEngine.INSTANCE);
        bus.register(OrganizationEngine.INSTANCE);
        bus.register(SpawnDirector.INSTANCE);
        bus.register(IncidentStoryEngine.INSTANCE);
        bus.register(SocietyStoryEngine.INSTANCE);
        bus.register(ContractEngine.INSTANCE);
        bus.register(OccultInvestigationEngine.INSTANCE);
        bus.register(InvestigationCommands.INSTANCE);
        bus.register(WorldEventDirector.INSTANCE);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CompatibilityManager.INSTANCE::initialize);
    }
}
