package com.ridelink.ride;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ride_events")
public class RideEvent {
    @Id public UUID id = UUID.randomUUID();
    public UUID rideId;
    public String status;
    public Instant occurredAt = Instant.now();
    public RideEvent() {}
    public RideEvent(UUID rideId, String status) { this.rideId = rideId; this.status = status; }
}
