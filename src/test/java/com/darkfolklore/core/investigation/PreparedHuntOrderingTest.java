package com.darkfolklore.core.investigation;

import com.darkfolklore.core.api.event.ConfirmedLivingDeathEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreparedHuntOrderingTest {
    @Test
    void preparedRewardObservesIdentifiedContractBeforeConfirmedDeathCompletion() throws Exception {
        SubscribeEvent subscription = OccultInvestigationEngine.class
                .getMethod("onPreparedHunt", ConfirmedLivingDeathEvent.class)
                .getAnnotation(SubscribeEvent.class);
        assertEquals(EventPriority.HIGH, subscription.priority());
    }
}
