package com.ridelink.ride;

import java.util.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RideRepository extends MongoRepository<Ride, UUID> {
    Optional<Ride> findByPassengerIdAndRequestKey(UUID passengerId, String requestKey);
    List<Ride> findByPassengerIdOrDriverIdOrderByCreatedAtDesc(UUID passengerId, UUID driverId);
}
