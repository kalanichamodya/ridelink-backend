package com.ridelink.ride;

import com.ridelink.common.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RideService {
    private final RideStore store;
    private final ServiceClient client;
    private final String driverUrl;
    private final String fareUrl;
    public RideService(RideStore store,ServiceClient client,@Value("${ridelink.driver-url}") String driverUrl,@Value("${ridelink.fare-url}") String fareUrl) {
        this.store=store; this.client=client; this.driverUrl=driverUrl; this.fareUrl=fareUrl;
    }
    public Ride create(Caller caller,String key,String pickup,String destination,String area,BigDecimal distance) {
        caller.require("PASSENGER");
        // Validate the fare contract before persisting. Distance is fictional, not a measured map distance.
        client.post(fareUrl+"/internal/fare-estimates",new Contracts.FareInput(pickup,destination,distance),Contracts.FareQuote.class);
        return store.create(caller.id(),key,pickup.trim(),destination.trim(),area.trim().toLowerCase(Locale.ROOT),distance);
    }
    public Ride assign(UUID id,Caller caller) {
        if (!caller.admin()) caller.require("PASSENGER");
        return store.mutate(id,r -> {
            caller.owns(r.passengerId);
            if (r.status==RideState.ASSIGNED) return r;
            transitionAllowed(r,RideState.ASSIGNED);
            var reservation=client.post(driverUrl+"/internal/reservations",new Contracts.ReserveDriver(r.id,r.serviceArea),Contracts.Reservation.class);
            r.driverId=Objects.requireNonNull(reservation).driverId(); r.status=RideState.ASSIGNED; return r;
        });
    }
    public Ride move(UUID id,Caller caller,RideState target) {
        Ride r=store.mutate(id,current -> {
            if (target==RideState.CANCELLED) {
                if (!caller.admin() && !caller.id().equals(current.passengerId) && !caller.id().equals(current.driverId))
                    throw new ApiException(403,"FORBIDDEN","Only the passenger or assigned driver may cancel");
            } else {
                caller.require("DRIVER");
                if (!caller.id().equals(current.driverId)) throw new ApiException(403,"FORBIDDEN","Only the assigned driver may update this ride");
            }
            if (current.status==target) return current; // Idempotent replay after a lost response.
            transitionAllowed(current,target);
            if (target==RideState.COMPLETED) {
                var charge=client.post(fareUrl+"/internal/charges",new Contracts.ChargeInput(current.id,current.passengerId,current.distanceKm),Contracts.ChargeView.class);
                current.finalFare=Objects.requireNonNull(charge).amount();
            }
            current.status=target; return current;
        });
        if (r.status==RideState.COMPLETED || r.status==RideState.CANCELLED) return release(id);
        return r;
    }
    private Ride release(UUID id) {
        // Status is committed before releasing. A repeat complete/cancel safely retries pending cleanup.
        return store.mutate(id,r -> {
            if (!r.driverReleased) {
                client.post(driverUrl+"/internal/reservations/release",new Contracts.ReleaseDriver(id),Map.class);
                r.driverReleased=true;
            }
            return r;
        });
    }
    private void transitionAllowed(Ride r,RideState target) {
        if (!r.status.allows(target)) throw new ApiException(409,"INVALID_RIDE_TRANSITION","Cannot change " + r.status + " to " + target);
    }
    public Ride get(UUID id,Caller caller) {
        Ride r=store.get(id);
        if (!caller.admin() && !caller.id().equals(r.passengerId) && !caller.id().equals(r.driverId))
            throw new ApiException(403,"FORBIDDEN","Ride belongs to another user");
        return r;
    }
    public List<Ride> list(Caller caller) { return store.list(caller.id()); }
    public List<RideEvent> history(UUID id,Caller caller) { get(id,caller); return store.history(id); }
    public Contracts.RideSummary summary(UUID id) { Ride r=store.get(id); return new Contracts.RideSummary(r.id,r.passengerId,r.driverId,r.status.name(),r.distanceKm); }
}
