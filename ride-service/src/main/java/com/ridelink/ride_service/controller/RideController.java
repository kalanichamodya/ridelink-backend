package com.ridelink.ride_service.controller;

import com.ridelink.ride_service.dto.AssignDriverRequest;
import com.ridelink.ride_service.dto.CreateRideRequest;
import com.ridelink.ride_service.dto.UpdateStatusRequest;
import com.ridelink.ride_service.entity.Ride;
import com.ridelink.ride_service.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    // POST /api/rides - Create a ride request (passenger)
    @PostMapping
    public ResponseEntity<?> createRide(@Valid @RequestBody CreateRideRequest request) {
        try {
            UUID passengerId = UUID.fromString(getLoggedInUserId());
            Ride ride = rideService.createRide(passengerId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(ride);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /api/rides/{id}/assign - Assign a driver
    @PatchMapping("/{id}/assign")
    public ResponseEntity<?> assignDriver(@PathVariable UUID id, @Valid @RequestBody AssignDriverRequest request) {
        try {
            Ride ride = rideService.assignDriver(id, request);
            return ResponseEntity.ok(ride);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /api/rides/{id}/status - Update ride status (lifecycle transition)
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        try {
            Ride ride = rideService.updateStatus(id, request);
            return ResponseEntity.ok(ride);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/rides/{id} - Get a ride by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getRide(@PathVariable UUID id) {
        try {
            Ride ride = rideService.getRide(id);
            return ResponseEntity.ok(ride);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/rides/passenger/{passengerId} - All rides for a passenger
    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<List<Ride>> getRidesForPassenger(@PathVariable UUID passengerId) {
        return ResponseEntity.ok(rideService.getRidesForPassenger(passengerId));
    }

    // GET /api/rides/driver/{driverId} - All rides for a driver
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<Ride>> getRidesForDriver(@PathVariable UUID driverId) {
        return ResponseEntity.ok(rideService.getRidesForDriver(driverId));
    }

    private String getLoggedInUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getPrincipal().toString() : null;
    }
}