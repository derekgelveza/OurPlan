package com.derekgelvez.calendar;

import org.springframework.boot.SpringApplication;
import com.derekgelvez.user.config.ApplicationConfiguration;
import com.derekgelvez.user.service.JwtService;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Runs the calendar feature. The User and UserProfile entities and their repositories
 * come from the user module; registering its package (alongside this one) lets JPA pick
 * them up without duplicating them.
 *
 * <p>{@code @Import} brings in only the user module's token check (JwtService) and account lookup
 * (ApplicationConfiguration), so this app accepts the JWTs the user app issues. The rest of the user
 * module (sign-up, email, its own security chain) is deliberately not loaded here.
 */
@SpringBootApplication
@AutoConfigurationPackage(basePackages = {"com.derekgelvez.calendar", "com.derekgelvez.user"})
@Import({ApplicationConfiguration.class, JwtService.class})
public class CalendarApplication {

    public static void main(String[] args) {
        SpringApplication.run(CalendarApplication.class, args);
    }

}
