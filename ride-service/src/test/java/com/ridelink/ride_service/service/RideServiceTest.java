package com.ridelink.ride_service.service;

import com.ridelink.ride_service.dto.AssignDriverRequest;
import com.ridelink.ride_service.dto.CreateRideRequest;
import com.ridelink.ride_service.dto.UpdateStatusRequest;
import com.ridelink.ride_service.entity.Ride;
import com.ridelink.ride_service.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @InjectMocks
    private RideService rideService;

    private Ride requestedRide;
    private UUID passengerId;
    private UUID driverId;

    @BeforeEach
    void setUp() {
        passengerId = UUID.randomUUID();
        driverId = UUID.randomUUID();

        requestedRide = new Ride();
        requestedRide.setId(UUID.randomUUID());
        requestedRide.setPassengerId(passengerId);
        requestedRide.setPickupLocation("Colombo Fort");
        requestedRide.setDestination("Negombo");
        requestedRide.setStatus(Ride.RideStatus.REQUESTED);
        requestedRide.setEstimatedFare(350.0);
    }

    @Test
    void createRide_shouldSaveWithRequestedStatus() {
        CreateRideRequest request = new CreateRideRequest();
        request.setPickupLocation("Colombo Fort");
        request.setDestination("Negombo");

        when(rideRepository.save(any(Ride.class))).thenReturn(requestedRide);

        Ride result = rideService.createRide(passengerId, request);

        assertEquals(Ride.RideStatus.REQUESTED, result.getStatus());
        assertNotNull(result.getEstimatedFare());
        verify(rideRepository, times(1)).save(any(Ride.class));
    }

    @Test
    void assignDriver_onRequestedRide_shouldSucceed() {
        AssignDriverRequest request = new AssignDriverRequest();
        request.setDriverId(driverId);

        when(rideRepository.findById(requestedRide.getId())).thenReturn(Optional.of(requestedRide));
        when(rideRepository.save(any(Ride.class))).thenReturn(requestedRide);

        Ride result = rideService.assignDriver(requestedRide.getId(), request);

        assertEquals(Ride.RideStatus.ASSIGNED, result.getStatus());
    }

    @Test
    void assignDriver_onAlreadyAssignedRide_shouldThrowException() {
        requestedRide.setStatus(Ride.RideStatus.ASSIGNED);
        AssignDriverRequest request = new AssignDriverRequest();
        request.setDriverId(driverId);

        when(rideRepository.findById(requestedRide.getId())).thenReturn(Optional.of(requestedRide));

        assertThrows(IllegalStateException.class,
                () -> rideService.assignDriver(requestedRide.getId(), request));
    }

    @Test
    void updateStatus_validTransition_shouldSucceed() {
        requestedRide.setStatus(Ride.RideStatus.ASSIGNED);
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("ACCEPTED");

        when(rideRepository.findById(requestedRide.getId())).thenReturn(Optional.of(requestedRide));
        when(rideRepository.save(any(Ride.class))).thenReturn(requestedRide);

        Ride result = rideService.updateStatus(requestedRide.getId(), request);

        assertEquals(Ride.RideStatus.ACCEPTED, result.getStatus());
    }

    @Test
    void updateStatus_invalidTransition_shouldThrowException() {
        requestedRide.setStatus(Ride.RideStatus.ASSIGNED);
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("COMPLETED");

        when(rideRepository.findById(requestedRide.getId())).thenReturn(Optional.of(requestedRide));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> rideService.updateStatus(requestedRide.getId(), request));

        assertTrue(exception.getMessage().contains("Invalid transition"));
    }

    @Test
    void updateStatus_toCompleted_shouldSetFinalFare() {
        requestedRide.setStatus(Ride.RideStatus.IN_PROGRESS);
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("COMPLETED");

        when(rideRepository.findById(requestedRide.getId())).thenReturn(Optional.of(requestedRide));
        when(rideRepository.save(any(Ride.class))).thenReturn(requestedRide);

        Ride result = rideService.updateStatus(requestedRide.getId(), request);

        assertEquals(Ride.RideStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getFinalFare());
    }

    @Test
    void getRide_nonExistent_shouldThrowException() {
        UUID randomId = UUID.randomUUID();
        when(rideRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> rideService.getRide(randomId));
    }
}