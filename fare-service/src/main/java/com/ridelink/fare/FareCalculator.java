package com.ridelink.fare;

import java.math.BigDecimal;
import java.math.RoundingMode;
/** LKR 150 base + LKR 85 per simulated kilometre; minimum LKR 250. */
public final class FareCalculator {
    private FareCalculator() {}
    public static BigDecimal calculate(BigDecimal distanceKm) {
        if (distanceKm == null || distanceKm.compareTo(new BigDecimal("0.1")) < 0 || distanceKm.compareTo(new BigDecimal("500")) > 0 || distanceKm.scale() > 2)
            throw new IllegalArgumentException("Distance must be 0.1 to 500 km, with at most two decimal places");
        return new BigDecimal("150").add(distanceKm.multiply(new BigDecimal("85"))).max(new BigDecimal("250")).setScale(2,RoundingMode.HALF_UP);
    }
}
