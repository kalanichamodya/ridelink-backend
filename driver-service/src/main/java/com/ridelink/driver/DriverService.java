package com.ridelink.driver;

import com.ridelink.common.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverService {
    private final DriverRepository repository;
    private final ServiceClient client;
    private final String accountUrl;
    public DriverService(DriverRepository repository,ServiceClient client,@Value("${ridelink.account-url}") String accountUrl) {
        this.repository=repository; this.client=client; this.accountUrl=accountUrl;
    }
    public Driver get(UUID id) { return repository.findById(id).orElseThrow(() -> ApiException.notFound("Driver profile")); }
    @Transactional public Driver save(UUID id,String plate,String model,String area,String location) {
        var current = repository.locked(id);
        Driver d=current.orElseGet(Driver::new);
        if (d.reservedRide != null) throw ApiException.conflict("Cannot change operational details during a reserved ride");
        d.id=id; d.plate=plate.trim().toUpperCase(Locale.ROOT); d.vehicleModel=model.trim();
        d.serviceArea=area.trim().toLowerCase(Locale.ROOT); d.location=location.trim();
        return repository.saveAndFlush(d);
    }
    @Transactional public Driver availability(UUID id,boolean available) {
        Driver d=repository.locked(id).orElseThrow(() -> ApiException.notFound("Driver profile"));
        if (d.reservedRide != null) throw ApiException.conflict("Driver has an active reservation");
        d.available=available; return repository.saveAndFlush(d);
    }
    public List<Driver> eligible(String area) {
        return repository.findByAvailableTrueAndReservedRideIsNullAndServiceAreaOrderByIdAsc(area.trim().toLowerCase(Locale.ROOT))
            .stream().filter(this::activeDriver).toList();
    }
    private boolean activeDriver(Driver d) {
        var state=client.get(accountUrl+"/internal/accounts/"+d.id,Contracts.AccountState.class);
        return state != null && state.active() && "DRIVER".equals(state.role());
    }
    @Transactional public Contracts.Reservation reserve(UUID rideId,String area) {
        var existing=repository.findByReservedRide(rideId);
        if (existing.isPresent()) return new Contracts.Reservation(existing.get().id,rideId);
        for (var candidate:repository.findByAvailableTrueAndReservedRideIsNullAndServiceAreaOrderByIdAsc(area.trim().toLowerCase(Locale.ROOT))) {
            if (activeDriver(candidate) && repository.claim(candidate.id,rideId)==1) {
                return new Contracts.Reservation(candidate.id,rideId);
            }
        }
        throw new ApiException(409,"NO_AVAILABLE_DRIVER","No eligible driver is available in this service area");
    }
    @Transactional public void release(UUID rideId) {
        repository.releaseReservation(rideId);
    }
}
