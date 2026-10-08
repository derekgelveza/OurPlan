package com.derekgelvez.calendar.config;

import com.derekgelvez.calendar.exception.CalendarAccessDeniedException;
import com.derekgelvez.user.model.User;
import com.derekgelvez.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Provides the id of the logged-in user to controllers. This is the single place that
 * knows how the authentication layer identifies a user: the principal name is either the
 * user's numeric id or their email.
 */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;
    private final boolean disableAuth;
    private final long bypassUserId;

    public CurrentUserProvider(UserRepository userRepository,
                               @Value("${DISABLE_AUTH:false}") boolean disableAuth,
                               @Value("${ourplan.security.bypass-user-id:1}") long bypassUserId) {
        this.userRepository = userRepository;
        this.disableAuth = disableAuth;
        this.bypassUserId = bypassUserId;
    }

    public Long getCurrentUserId() {
        if (disableAuth) {
            // TEMP-AUTH-DISABLED: Resolve every anonymous request to one configurable local test user.
            return userRepository.findById(bypassUserId)
                    .map(User::getId)
                    .orElseGet(this::createBypassUser);
        }

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

    // TEMP-AUTH-DISABLED: Keep identity-dependent features usable when the local test DB is empty.
    private Long createBypassUser() {
        return userRepository.findByEmail("test-user@ourplan.local")
                .map(User::getId)
                .orElseGet(() -> userRepository.save(new User("test-user@ourplan.local")).getId());
    }
}
