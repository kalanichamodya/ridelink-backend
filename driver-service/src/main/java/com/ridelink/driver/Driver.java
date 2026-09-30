package com.ridelink.driver;

import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="drivers",uniqueConstraints=@UniqueConstraint(columnNames="plate"))
public class Driver {
    @Id public UUID id;
    @Column(nullable=false) public String plate;
    @Column(nullable=false) public String vehicleModel;
    @Column(nullable=false) public String serviceArea;
    @Column(nullable=false) public String location;
    public boolean available;
    @Column(unique=true) public UUID reservedRide;
    @Version public long version;
}
