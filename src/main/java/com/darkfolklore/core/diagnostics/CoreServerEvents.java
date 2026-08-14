package com.darkfolklore.core.diagnostics;

import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.compat.l2hostility.L2HostilityBridge;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.encounter.RitualEngine;
import com.darkfolklore.core.encounter.ThreatPolicyManager;
import com.darkfolklore.core.encounter.ThreatPolicyRuntime;
import com.darkfolklore.core.lifecycle.McaVampireLifecycleEngine;
import com.darkfolklore.core.predation.PredationTraceEngine;
import com.darkfolklore.core.predation.VampirePredationEngine;
import com.darkfolklore.core.society.rumor.RumorEngine;
import com.darkfolklore.core.society.village.VillageResponseEngine;
import com.darkfolklore.core.society.witness.WitnessEngine;
import com.darkfolklore.core.world.WorldEventDirector;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class CoreServerEvents {
    public static final CoreServerEvents INSTANCE = new CoreServerEvents();
    private CoreServerEvents() {}

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(FolkloreDataManager.INSTANCE);
        event.addListener(ThreatPolicyManager.INSTANCE);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        FolkloreCommands.register(event.getDispatcher()); InvestigationCommands.register(event.getDispatcher());
        PredationCommands.register(event.getDispatcher()); LifecycleCommands.register(event.getDispatcher());
        KnowledgeCommands.register(event.getDispatcher()); SocietyCommands.register(event.getDispatcher());
        MagicCommands.register(event.getDispatcher()); WorldLoopCommands.register(event.getDispatcher());
        EncounterCommands.register(event.getDispatcher()); CasebookCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        CompatibilityManager.INSTANCE.clearRuntimeCaches();
        McaVampireLifecycleEngine.INSTANCE.clearRuntimeState();
        VampirePredationEngine.INSTANCE.clearRuntimeState();
        PredationTraceEngine.INSTANCE.clearRuntimeState();
        RumorEngine.INSTANCE.clearRuntimeState();
        WitnessEngine.INSTANCE.clearRuntimeState();
        WorldEventDirector.INSTANCE.clearRuntimeState();
        VillageResponseEngine.INSTANCE.clearRuntimeState();
        ThreatPolicyRuntime.INSTANCE.clearRuntimeState();
        RitualEngine.clearRuntimeState();
        L2HostilityBridge.INSTANCE.reset();
    }
}
