package com.ridelink.fare;

import com.ridelink.common.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FareService {
    private final ChargeRepository charges;
    private final PaymentRepository payments;
    private final ServiceClient client;
    private final String rideUrl;
    public FareService(ChargeRepository charges,PaymentRepository payments,ServiceClient client,@Value("${ridelink.ride-url}") String rideUrl) {
        this.charges=charges; this.payments=payments; this.client=client; this.rideUrl=rideUrl;
    }
    public Contracts.FareQuote estimate(BigDecimal distance) {
        try { return new Contracts.FareQuote(FareCalculator.calculate(distance),"LKR","max(250, 150 + 85 * distanceKm), rounded HALF_UP to 2 decimals"); }
        catch (IllegalArgumentException e) { throw new ApiException(400,"INVALID_INPUT",e.getMessage()); }
    }
    @Transactional public Contracts.ChargeView charge(UUID rideId,UUID passenger,BigDecimal distance) {
        BigDecimal amount=estimate(distance).amount();
        var existing=charges.findById(rideId);
        if (existing.isPresent()) {
            Charge c=existing.get();
            if (!c.passengerId.equals(passenger) || c.distanceKm.compareTo(distance)!=0) throw ApiException.conflict("Charge input differs from existing charge");
            return view(c);
        }
        Charge c=new Charge(); c.rideId=rideId; c.passengerId=passenger; c.distanceKm=distance; c.amount=amount;
        return view(charges.saveAndFlush(c));
    }
    private Contracts.ChargeView view(Charge c) { return new Contracts.ChargeView(c.rideId,c.passengerId,c.amount,c.currency); }
    public Contracts.ChargeView getCharge(UUID id,Caller caller) {
        Charge c=charges.findById(id).orElseThrow(() -> ApiException.notFound("Charge")); caller.owns(c.passengerId); return view(c);
    }
    @Transactional public Payment pay(UUID rideId,String key,boolean fail,Caller caller) {
        caller.require("PASSENGER");
        // Serialize all attempts for one ride, including successful requests with different keys.
        Charge c=charges.locked(rideId).orElseThrow(() -> ApiException.notFound("Final charge"));
        caller.owns(c.passengerId);
        var replay=payments.findByPassengerIdAndRequestKey(caller.id(),key);
        if (replay.isPresent()) {
            Payment p=replay.get();
            if (!p.rideId.equals(rideId) || !p.status.equals(fail ? "FAILED" : "SUCCEEDED")) throw ApiException.conflict("Payment key was used with a different request");
            return p;
        }
        var ride=client.get(rideUrl+"/internal/rides/"+rideId,Contracts.RideSummary.class);
        if (ride==null || !"COMPLETED".equals(ride.status()) || !caller.id().equals(ride.passengerId()))
            throw ApiException.conflict("Payment requires the passenger's completed ride");
        if (payments.findByRideIdAndStatus(rideId,"SUCCEEDED").isPresent()) throw ApiException.conflict("Ride is already paid");
        Payment p=new Payment(); p.id=UUID.randomUUID(); p.rideId=rideId; p.passengerId=caller.id(); p.requestKey=key;
        p.status=fail ? "FAILED" : "SUCCEEDED"; p.amount=c.amount;
        return payments.saveAndFlush(p);
    }
    public Payment getPayment(UUID id,Caller caller) {
        Payment p=payments.findById(id).orElseThrow(() -> ApiException.notFound("Payment")); caller.owns(p.passengerId); return p;
    }
    public Map<String,Object> receipt(UUID id,Caller caller) {
        Payment p=getPayment(id,caller);
        if (!"SUCCEEDED".equals(p.status)) throw ApiException.conflict("Failed payment has no receipt");
        return Map.of("receiptNumber","RL-"+p.id,"paymentId",p.id,"rideId",p.rideId,"passengerId",p.passengerId,
            "amount",p.amount,"currency",p.currency,"paidAt",p.createdAt,"simulated",true);
    }
}
