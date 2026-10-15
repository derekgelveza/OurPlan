package com.derekgelvez.user.responses;

import com.derekgelvez.user.model.User;

/**
 * The public view of an account. Never return the {@code User} entity itself: it contains the
 * password hash and the email verification code.
 */
public record UserResponse(Long id, String username, String email, boolean verified) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getHandle(), user.getEmail(), user.isEnabled());
    }
}
