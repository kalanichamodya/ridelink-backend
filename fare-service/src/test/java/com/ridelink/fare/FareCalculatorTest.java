package com.ridelink.fare;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FareCalculatorTest {
    @Test void minimumFareApplies() { assertEquals(new BigDecimal("250.00"),FareCalculator.calculate(new BigDecimal("0.1"))); }
    @Test void fiveKilometresCosts575() { assertEquals(new BigDecimal("575.00"),FareCalculator.calculate(new BigDecimal("5"))); }
    @Test void fractionalDistancePreservesCents() { assertEquals(new BigDecimal("254.55"),FareCalculator.calculate(new BigDecimal("1.23"))); }
    @Test void maximumDistanceIsAccepted() { assertEquals(new BigDecimal("42650.00"),FareCalculator.calculate(new BigDecimal("500"))); }
    @Test void outOfRangeAndOverPrecisionAreRejected() {
        for (String s:new String[]{"-1","0","0.09","500.01","1.234"})
            assertThrows(IllegalArgumentException.class,()->FareCalculator.calculate(new BigDecimal(s)));
        assertThrows(IllegalArgumentException.class,()->FareCalculator.calculate(null));
    }
}
