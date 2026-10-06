package com.derekgelvez.calendar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Runs the calendar feature. The User and UserProfile entities and their repositories
 * come from the user module; registering its package (alongside this one) lets JPA pick
 * them up without duplicating them.
 */
@SpringBootApplication
@AutoConfigurationPackage(basePackages = {"com.derekgelvez.calendar", "com.derekgelvez.user"})
public class CalendarApplication {

    public static void main(String[] args) {
        SpringApplication.run(CalendarApplication.class, args);
    }

}
