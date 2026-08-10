package com.darkfolklore.core.api.event;

import com.darkfolklore.core.contracts.ContractAssignment;
import net.neoforged.bus.api.Event;

public final class ContractStartedEvent extends Event {
    private final ContractAssignment assignment;
    public ContractStartedEvent(ContractAssignment assignment) { this.assignment = assignment; }
    public ContractAssignment assignment() { return assignment; }
}
