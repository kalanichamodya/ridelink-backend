package com.ridelink.ride;

import com.ridelink.common.*;
import java.util.*;
import java.math.BigDecimal;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RideServiceTest {
    RideStore store;ServiceClient client;RideService service;Ride ride;Caller driver;Caller passenger;
    @BeforeEach void setup() {
        store=mock(RideStore.class);client=mock(ServiceClient.class);service=new RideService(store,client,"http://driver","http://fare");
        ride=new Ride();ride.id=UUID.randomUUID();ride.passengerId=UUID.randomUUID();ride.driverId=UUID.randomUUID();ride.distanceKm=new BigDecimal("5");
        driver=new Caller(ride.driverId,"DRIVER");passenger=new Caller(ride.passengerId,"PASSENGER");
        when(store.get(ride.id)).thenReturn(ride);
        when(store.mutate(eq(ride.id),any())).thenAnswer(i -> ((Function<Ride,Ride>)i.getArgument(1)).apply(ride));
    }
    @Test void assignedRideCannotStartBeforeAcceptance() {
        ride.status=RideState.ASSIGNED;
        assertEquals("INVALID_RIDE_TRANSITION",assertThrows(ApiException.class,()->service.move(ride.id,driver,RideState.IN_PROGRESS)).code);
    }
    @Test void passengerCannotAcceptRide() {
        ride.status=RideState.ASSIGNED;
        assertEquals(403,assertThrows(ApiException.class,()->service.move(ride.id,passenger,RideState.ACCEPTED)).status);
    }
    @Test void unassignedDriverCannotAcceptRide() {
        ride.status=RideState.ASSIGNED;
        assertEquals(403,assertThrows(ApiException.class,()->service.move(ride.id,new Caller(UUID.randomUUID(),"DRIVER"),RideState.ACCEPTED)).status);
    }
    @Test void acceptanceReplayIsSafe() {
        ride.status=RideState.ASSIGNED;service.move(ride.id,driver,RideState.ACCEPTED);
        assertEquals(RideState.ACCEPTED,service.move(ride.id,driver,RideState.ACCEPTED).status);
    }
    @Test void completionCalculatesFareAndReleasesDriver() {
        ride.status=RideState.IN_PROGRESS;
        when(client.post(eq("http://fare/internal/charges"),any(),eq(Contracts.ChargeView.class)))
            .thenReturn(new Contracts.ChargeView(ride.id,ride.passengerId,new BigDecimal("575.00"),"LKR"));
        Ride result=service.move(ride.id,driver,RideState.COMPLETED);
        assertEquals(new BigDecimal("575.00"),result.finalFare);assertTrue(result.driverReleased);
        verify(client).post(eq("http://driver/internal/reservations/release"),any(),eq(Map.class));
    }
    @Test void failedChargeDoesNotCompleteRide() {
        ride.status=RideState.IN_PROGRESS;
        when(client.post(anyString(),any(),eq(Contracts.ChargeView.class))).thenThrow(new ApiException(503,"DEPENDENCY_UNAVAILABLE","offline"));
        assertThrows(ApiException.class,()->service.move(ride.id,driver,RideState.COMPLETED));
        assertEquals(RideState.IN_PROGRESS,ride.status);
    }
    @Test void completedReplayRetriesPendingReleaseWithoutAnotherCharge() {
        ride.status=RideState.COMPLETED;ride.driverReleased=false;
        assertTrue(service.move(ride.id,driver,RideState.COMPLETED).driverReleased);
        verify(client,never()).post(eq("http://fare/internal/charges"),any(),eq(Contracts.ChargeView.class));
    }
    @Test void inProgressCannotBeCancelled() {
        ride.status=RideState.IN_PROGRESS;
        assertEquals(409,assertThrows(ApiException.class,()->service.move(ride.id,passenger,RideState.CANCELLED)).status);
    }
    @Test void strangerCannotReadRide() {
        assertEquals(403,assertThrows(ApiException.class,()->service.get(ride.id,new Caller(UUID.randomUUID(),"PASSENGER"))).status);
    }
}
