package com.derekgelvez.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the user (auth) service: sign-up, email verification, login and password reset.
 * It issues the JWTs that the calendar service also accepts. Listens on port 8081 by default so it
 * can run alongside the calendar service on 8080.
 */
@SpringBootApplication
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }

}
