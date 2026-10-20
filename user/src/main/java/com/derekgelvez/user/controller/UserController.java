package com.derekgelvez.user.controller;


import com.derekgelvez.user.model.User;
import com.derekgelvez.user.responses.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for the signed-in user. Every route here requires a valid JWT; the principal is the
 * {@code User} that JwtAuthenticationFilter placed in the security context.
 */
@RequestMapping("/users")
@RestController
public class UserController {

    /** The caller's own account, returned as a {@link UserResponse} so no secrets leave the server. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> authenticatedUser(@AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(UserResponse.from(currentUser));
    }

    // account's email, password hash and verification code. Add it back only behind an admin role.
}
