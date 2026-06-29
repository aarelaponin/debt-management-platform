package com.fiscaladmin.mtca.cmbb.phase;

import com.fiscaladmin.mtca.cmbb.service.GuardContext;
import com.fiscaladmin.mtca.cmbb.service.InvalidTransitionException;

/** One guard phase of the case envelope (PL-TransitionGuard.md). */
public interface GuardPhase {

    /** @throws InvalidTransitionException when the transition must be rejected */
    void run(GuardContext ctx) throws InvalidTransitionException;
}
