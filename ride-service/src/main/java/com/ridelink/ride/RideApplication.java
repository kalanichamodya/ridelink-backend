package com.ridelink.ride;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.ridelink.ride", "com.ridelink.common"})
public class RideApplication {
    public static void main(String[] args) { SpringApplication.run(RideApplication.class, args); }
}
