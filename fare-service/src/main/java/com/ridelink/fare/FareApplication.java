package com.ridelink.fare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.ridelink.fare", "com.ridelink.common"})
public class FareApplication {
    public static void main(String[] args) { SpringApplication.run(FareApplication.class, args); }
}
