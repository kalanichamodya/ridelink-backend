package com.ridelink.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.ridelink.account", "com.ridelink.common"})
public class AccountApplication {
    public static void main(String[] args) { SpringApplication.run(AccountApplication.class, args); }
}
