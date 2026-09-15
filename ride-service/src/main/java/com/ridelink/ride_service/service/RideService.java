package com.ridelink.ride_service.service;

import com.ridelink.ride_service.dto.AssignDriverRequest;
import com.ridelink.ride_service.dto.CreateRideRequest;
import com.ridelink.ride_service.dto.UpdateStatusRequest;
import com.ridelink.ride_service.entity.Ride;
import com.ridelink.ride_service.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class RideService {

    private final RideRepository rideRepository;

    // Documented simple fare rule: base fare + per-km rate (assumed distance placeholder for demo)
    private static final double BASE_FARE = 100.0;
    private static final double PER_KM_RATE = 50.0;

    // Valid status transitions map
    private static final Map<Ride.RideStatus, Set<Ride.RideStatus>> VALID_TRANSITIONS = Map.of(
            Ride.RideStatus.REQUESTED, Set.of(Ride.RideStatus.ASSIGNED, Ride.RideStatus.CANCELLED),
            Ride.RideStatus.ASSIGNED, Set.of(Ride.RideStatus.ACCEPTED, Ride.RideStatus.CANCELLED),
            Ride.RideStatus.ACCEPTED, Set.of(Ride.RideStatus.IN_PROGRESS, Ride.RideStatus.CANCELLED),
            Ride.RideStatus.IN_PROGRESS, Set.of(Ride.RideStatus.COMPLETED, Ride.RideStatus.CANCELLED),
            Ride.RideStatus.COMPLETED, Set.of(),
            Ride.RideStatus.CANCELLED, Set.of()
    );

    public RideService(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    public Ride createRide(UUID passengerId, CreateRideRequest request) {
        Ride ride = new Ride();
        ride.setPassengerId(passengerId);
        ride.setPickupLocation(request.getPickupLocation());
        ride.setDestination(request.getDestination());
        ride.setStatus(Ride.RideStatus.REQUESTED);

        // Simple estimated fare: base + a fixed assumed distance factor (documented rule for demo purposes)
        double estimatedDistanceKm = 5.0;
        ride.setEstimatedFare(BASE_FARE + (estimatedDistanceKm * PER_KM_RATE));

        return rideRepository.save(ride);
    }

    public Ride assignDriver(UUID rideId, AssignDriverRequest request) {
        Ride ride = getRideOrThrow(rideId);

        if (ride.getStatus() != Ride.RideStatus.REQUESTED) {
            throw new IllegalStateException("Cannot assign a driver unless ride is in REQUESTED status");
        }

        ride.setDriverId(request.getDriverId());
        ride.setStatus(Ride.RideStatus.ASSIGNED);
        ride.setUpdatedAt(LocalDateTime.now());

        return rideRepository.save(ride);
    }

    public Ride updateStatus(UUID rideId, UpdateStatusRequest request) {
        Ride ride = getRideOrThrow(rideId);

        Ride.RideStatus newStatus;
        try {
            newStatus = Ride.RideStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value: " + request.getStatus());
        }

        Set<Ride.RideStatus> allowedNext = VALID_TRANSITIONS.get(ride.getStatus());
        if (allowedNext == null || !allowedNext.contains(newStatus)) {
            throw new IllegalStateException(
                    "Invalid transition from " + ride.getStatus() + " to " + newStatus
            );
        }

        ride.setStatus(newStatus);
        ride.setUpdatedAt(LocalDateTime.now());

        // On completion, calculate final fare (same simple rule, could use actual distance later)
        if (newStatus == Ride.RideStatus.COMPLETED) {
            ride.setFinalFare(ride.getEstimatedFare());
        }

        return rideRepository.save(ride);
    }

    public Ride getRide(UUID rideId) {
        return getRideOrThrow(rideId);
    }

    public List<Ride> getRidesForPassenger(UUID passengerId) {
        return rideRepository.findByPassengerId(passengerId);
    }

    public List<Ride> getRidesForDriver(UUID driverId) {
        return rideRepository.findByDriverId(driverId);
    }

    private Ride getRideOrThrow(UUID rideId) {
        Optional<Ride> ride = rideRepository.findById(rideId);
        if (ride.isEmpty()) {
            throw new IllegalArgumentException("Ride not found");
        }
        return ride.get();
    }
}