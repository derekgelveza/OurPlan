package com.derekgelvez.user.responses;


import lombok.Getter;
import lombok.Setter;

/** Returned by {@code /auth/login}: the JWT and its lifetime in milliseconds. */
@Getter
@Setter
public class LoginResponse {

    private String token;

    private long expiresIn;

    public LoginResponse(long expiresIn, String token) {
        this.expiresIn = expiresIn;
        this.token = token;
    }
}
