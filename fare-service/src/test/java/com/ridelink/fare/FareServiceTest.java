package com.ridelink.fare;

import com.ridelink.common.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FareServiceTest {
    ChargeRepository charges;PaymentRepository payments;ServiceClient client;FareService service;Charge charge;Caller passenger;
    @BeforeEach void setup() {
        charges=mock(ChargeRepository.class);payments=mock(PaymentRepository.class);client=mock(ServiceClient.class);
        service=new FareService(charges,payments,client,"http://ride");charge=new Charge();charge.rideId=UUID.randomUUID();charge.passengerId=UUID.randomUUID();charge.amount=new BigDecimal("575.00");
        passenger=new Caller(charge.passengerId,"PASSENGER");
        when(charges.locked(charge.rideId)).thenReturn(Optional.of(charge));
        when(payments.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
        when(client.get(anyString(),eq(Contracts.RideSummary.class))).thenReturn(new Contracts.RideSummary(charge.rideId,charge.passengerId,UUID.randomUUID(),"COMPLETED",new BigDecimal("5")));
    }
    @Test void successfulPaymentUsesAuthoritativeCharge() {
        Payment p=service.pay(charge.rideId,"key",false,passenger);
        assertEquals("SUCCEEDED",p.status);assertEquals(new BigDecimal("575.00"),p.amount);
    }
    @Test void failedSimulationIsRecorded() {
        assertEquals("FAILED",service.pay(charge.rideId,"key",true,passenger).status);
    }
    @Test void duplicateSuccessWithDifferentKeyIsRejected() {
        when(payments.findByRideIdAndStatus(charge.rideId,"SUCCEEDED")).thenReturn(Optional.of(new Payment()));
        assertEquals(409,assertThrows(ApiException.class,()->service.pay(charge.rideId,"other",false,passenger)).status);
    }
    @Test void replayReturnsOriginalPayment() {
        Payment p=new Payment();p.id=UUID.randomUUID();p.rideId=charge.rideId;p.status="SUCCEEDED";
        when(payments.findByPassengerIdAndRequestKey(passenger.id(),"key")).thenReturn(Optional.of(p));
        assertEquals(p.id,service.pay(charge.rideId,"key",false,passenger).id);
        verify(payments,never()).saveAndFlush(any());
    }
    @Test void inProgressRideCannotBePaid() {
        when(client.get(anyString(),eq(Contracts.RideSummary.class))).thenReturn(new Contracts.RideSummary(charge.rideId,charge.passengerId,null,"IN_PROGRESS",new BigDecimal("5")));
        assertEquals(409,assertThrows(ApiException.class,()->service.pay(charge.rideId,"key",false,passenger)).status);
    }
    @Test void otherPassengerCannotPay() {
        assertEquals(403,assertThrows(ApiException.class,()->service.pay(charge.rideId,"key",false,new Caller(UUID.randomUUID(),"PASSENGER"))).status);
    }
    @Test void failedPaymentHasNoReceipt() {
        Payment p=new Payment();p.id=UUID.randomUUID();p.passengerId=passenger.id();p.status="FAILED";
        when(payments.findById(p.id)).thenReturn(Optional.of(p));
        assertEquals(409,assertThrows(ApiException.class,()->service.receipt(p.id,passenger)).status);
    }
    @Test void invalidEstimateIsClientError() {
        assertEquals(400,assertThrows(ApiException.class,()->service.estimate(BigDecimal.ZERO)).status);
    }
}
