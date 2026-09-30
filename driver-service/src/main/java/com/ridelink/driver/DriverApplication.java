package com.ridelink.driver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.ridelink.driver", "com.ridelink.common"})
public class DriverApplication {
    public static void main(String[] args) { SpringApplication.run(DriverApplication.class, args); }
}
