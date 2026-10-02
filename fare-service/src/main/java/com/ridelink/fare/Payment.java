package com.ridelink.fare;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="payments",uniqueConstraints=@UniqueConstraint(columnNames={"passengerId","requestKey"}))
public class Payment {
    @Id public UUID id;
    @Column(nullable=false) public UUID rideId;
    @Column(nullable=false) public UUID passengerId;
    @Column(nullable=false) public String requestKey;
    @Column(nullable=false) public String status;
    @Column(nullable=false,precision=12,scale=2) public BigDecimal amount;
    public String currency="LKR";
    public Instant createdAt=Instant.now();
}
