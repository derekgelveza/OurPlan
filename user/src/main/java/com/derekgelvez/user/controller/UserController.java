package com.derekgelvez.user.controller;


import com.derekgelvez.user.model.User;
import com.derekgelvez.user.responses.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/users")
@RestController
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserResponse> authenticatedUser(@AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(UserResponse.from(currentUser));
    }

    // GET /users/ (list every account) was removed: any signed-in user could read every
    // account's email, password hash and verification code. Add it back only behind an admin role.
}
