package com.ridelink.ride_service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class AssignDriverRequest {

    @NotNull(message = "Driver ID is required")
    private UUID driverId;

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }
}