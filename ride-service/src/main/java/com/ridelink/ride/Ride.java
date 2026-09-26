package com.ridelink.ride;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "rides")
@CompoundIndex(name = "passenger_requestKey_unique", def = "{'passengerId': 1, 'requestKey': 1}", unique = true)
public class Ride {
    @Id public UUID id;
    public UUID passengerId;
    public UUID driverId;
    public String requestKey;
    public String pickup;
    public String destination;
    public String serviceArea;
    public BigDecimal distanceKm;
    public BigDecimal finalFare;
    public RideState status = RideState.REQUESTED;
    public Instant createdAt = Instant.now();
    public boolean driverReleased;
    @Version public Long version;
}
