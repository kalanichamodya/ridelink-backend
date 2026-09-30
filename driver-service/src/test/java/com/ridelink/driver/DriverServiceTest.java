package com.ridelink.driver;

import com.ridelink.common.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DriverServiceTest {
    DriverRepository repository; ServiceClient client; DriverService service; Driver driver; UUID ride;
    @BeforeEach void setup() {
        repository=mock(DriverRepository.class);client=mock(ServiceClient.class);service=new DriverService(repository,client,"http://account");
        driver=new Driver();driver.id=UUID.randomUUID();driver.available=true;ride=UUID.randomUUID();
        when(repository.locked(driver.id)).thenReturn(Optional.of(driver));
        when(repository.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
        when(client.get("http://account/internal/accounts/"+driver.id,Contracts.AccountState.class)).thenReturn(new Contracts.AccountState(driver.id,"DRIVER",true));
    }
    @Test void noAvailableDriverReturnsConflict() {
        ApiException error=assertThrows(ApiException.class,()->service.reserve(ride,"colombo"));
        assertEquals("NO_AVAILABLE_DRIVER",error.code);
    }
    @Test void reservationUsesAtomicClaim() {
        when(repository.findByAvailableTrueAndReservedRideIsNullAndServiceAreaOrderByIdAsc("colombo")).thenReturn(List.of(driver));
        when(repository.claim(driver.id,ride)).thenReturn(1);
        assertEquals(driver.id,service.reserve(ride," Colombo ").driverId());
        verify(repository).claim(driver.id,ride);
    }
    @Test void lostClaimCannotDoubleAssign() {
        when(repository.findByAvailableTrueAndReservedRideIsNullAndServiceAreaOrderByIdAsc("colombo")).thenReturn(List.of(driver));
        when(repository.claim(driver.id,ride)).thenReturn(0);
        assertThrows(ApiException.class,()->service.reserve(ride,"colombo"));
    }
    @Test void replayReturnsExistingReservation() {
        driver.reservedRide=ride;when(repository.findByReservedRide(ride)).thenReturn(Optional.of(driver));
        assertEquals(driver.id,service.reserve(ride,"colombo").driverId());
        verify(repository,never()).claim(any(),any());
    }
    @Test void disabledAccountIsNotEligible() {
        when(repository.findByAvailableTrueAndReservedRideIsNullAndServiceAreaOrderByIdAsc("colombo")).thenReturn(List.of(driver));
        when(client.get(anyString(),eq(Contracts.AccountState.class))).thenReturn(new Contracts.AccountState(driver.id,"DRIVER",false));
        assertThrows(ApiException.class,()->service.reserve(ride,"colombo"));
        verify(repository,never()).claim(any(),any());
    }
    @Test void reservedDriverCannotToggleAvailability() {
        driver.reservedRide=ride;assertEquals(409,assertThrows(ApiException.class,()->service.availability(driver.id,true)).status);
    }
    @Test void releaseIsRepeatableAndDoesNotReleaseAnotherRide() {
        when(repository.releaseReservation(ride)).thenReturn(1,0);
        service.release(ride);service.release(ride);
        verify(repository,times(2)).releaseReservation(ride);
    }
}
