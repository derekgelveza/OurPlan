package com.derekgelvez.calendar.config;

import com.derekgelvez.calendar.exception.CalendarAccessDeniedException;
import com.derekgelvez.user.model.User;
import com.derekgelvez.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Provides the id of the logged-in user to controllers. This is the single place that
 * knows how the authentication layer identifies a user: the principal name is either the
 * user's numeric id or their email.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new CalendarAccessDeniedException("No authenticated user");
        }
        String principal = authentication.getName().trim();
        if (principal.chars().allMatch(Character::isDigit) && !principal.isEmpty()) {
            return Long.valueOf(principal);
        }
        return userRepository.findByEmail(principal.toLowerCase(Locale.ROOT))
                .map(User::getId)
                .orElseThrow(() -> new CalendarAccessDeniedException("Authenticated user is not an OurPlan user"));
    }
}
