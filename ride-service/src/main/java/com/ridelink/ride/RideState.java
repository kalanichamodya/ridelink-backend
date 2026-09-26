package com.ridelink.ride;

import java.util.Set;
/** Pure business rule, independent of HTTP and persistence. */
public enum RideState {
    REQUESTED, ASSIGNED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED;
    public boolean allows(RideState next) {
        return switch (this) {
            case REQUESTED -> Set.of(ASSIGNED,CANCELLED).contains(next);
            case ASSIGNED -> Set.of(ACCEPTED,CANCELLED).contains(next);
            case ACCEPTED -> Set.of(IN_PROGRESS,CANCELLED).contains(next);
            case IN_PROGRESS -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
