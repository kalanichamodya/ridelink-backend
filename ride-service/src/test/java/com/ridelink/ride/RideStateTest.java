package com.ridelink.ride;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RideStateTest {
    @Test void onlySevenForwardEdgesAreAllowed() {
        int allowed=0;
        for (RideState from:RideState.values()) for (RideState to:RideState.values()) if (from.allows(to)) allowed++;
        assertEquals(7,allowed);
    }
    @Test void terminalStatesNeverTransition() {
        for (RideState to:RideState.values()) { assertFalse(RideState.COMPLETED.allows(to)); assertFalse(RideState.CANCELLED.allows(to)); }
    }
}
