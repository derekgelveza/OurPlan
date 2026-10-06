package com.derekgelvez.calendar.service;

import com.derekgelvez.calendar.exception.ResourceNotFoundException;
import com.derekgelvez.user.model.User;
import com.derekgelvez.user.repository.UserProfileRepository;
import com.derekgelvez.user.repository.UserRepository;
import com.derekgelvez.user.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Calendar-side helper over the user module's repositories. */
@Component
@RequiredArgsConstructor
class UserLookup {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + userId + " not found"));
    }

    /** The user's display name, falling back to their email when they have no profile. */
    String displayName(User user) {
        return userProfileRepository.findByUserId(user.getId())
                .map(UserProfile::getDisplayName)
                .orElse(user.getEmail());
    }
}
