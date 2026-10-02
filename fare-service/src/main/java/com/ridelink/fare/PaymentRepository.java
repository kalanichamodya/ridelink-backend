package com.ridelink.fare;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentRepository extends JpaRepository<Payment,UUID> {
    Optional<Payment> findByPassengerIdAndRequestKey(UUID passengerId,String requestKey);
    Optional<Payment> findByRideIdAndStatus(UUID rideId,String status);
}
