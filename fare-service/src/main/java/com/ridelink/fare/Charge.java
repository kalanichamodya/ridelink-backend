package com.ridelink.fare;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity
public class Charge {
    @Id public UUID rideId;
    @Column(nullable=false) public UUID passengerId;
    @Column(nullable=false,precision=12,scale=2) public BigDecimal amount;
    @Column(nullable=false,precision=10,scale=2) public BigDecimal distanceKm;
    public String currency="LKR";
    @Version public long version;
}
