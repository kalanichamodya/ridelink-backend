package com.ridelink.ride;

import java.util.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RideEventRepository extends MongoRepository<RideEvent, UUID> {
    List<RideEvent> findByRideIdOrderByOccurredAtAsc(UUID rideId);
}
