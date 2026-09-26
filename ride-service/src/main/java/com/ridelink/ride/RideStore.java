package com.ridelink.ride;

import com.ridelink.common.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/** Commits REQUESTED independently before remote assignment; retries reuse the persisted ride ID.
 *  MongoDB's @Version field on Ride gives optimistic-locking protection on concurrent updates
 *  (save() throws if another writer changed the document first) instead of a pessimistic DB lock. */
@Service
public class RideStore {
    private final RideRepository repository;
    private final RideEventRepository events;
    public RideStore(RideRepository repository,RideEventRepository events) { this.repository=repository; this.events=events; }
    public Ride create(UUID passenger,String key,String pickup,String destination,String area,BigDecimal distance) {
        var existing=repository.findByPassengerIdAndRequestKey(passenger,key);
        if (existing.isPresent()) {
            Ride r=existing.get();
            if (!r.pickup.equals(pickup) || !r.destination.equals(destination) || !r.serviceArea.equals(area) || r.distanceKm.compareTo(distance)!=0)
                throw ApiException.conflict("Idempotency key was already used with a different request");
            return r;
        }
        Ride r=new Ride(); r.id=UUID.randomUUID(); r.passengerId=passenger; r.requestKey=key; r.pickup=pickup;
        r.destination=destination; r.serviceArea=area; r.distanceKm=distance;
        repository.save(r); events.save(new RideEvent(r.id,r.status.name())); return r;
    }
    public Ride mutate(UUID id,Function<Ride,Ride> action) {
        Ride r=repository.findById(id).orElseThrow(() -> ApiException.notFound("Ride"));
        RideState before=r.status;
        Ride result=action.apply(r);
        if (before!=result.status) events.save(new RideEvent(id,result.status.name()));
        return repository.save(result);
    }
    public Ride get(UUID id) { return repository.findById(id).orElseThrow(() -> ApiException.notFound("Ride")); }
    public List<Ride> list(UUID id) { return repository.findByPassengerIdOrDriverIdOrderByCreatedAtDesc(id,id); }
    public List<RideEvent> history(UUID id) { return events.findByRideIdOrderByOccurredAtAsc(id); }
}
