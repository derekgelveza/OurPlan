package com.derekgelvez.calendar.config;

import com.derekgelvez.calendar.exception.CalendarAccessDeniedException;
import com.derekgelvez.user.model.User;
import com.derekgelvez.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Provides the id of the logged-in user to controllers. This is the single place that
 * knows how the authentication layer identifies a user: the JWT filter stores the User
 * entity as the principal, and its name is the user's email.
 */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;
    private final boolean disableAuth;
    private final long bypassUserId;

    public CurrentUserProvider(UserRepository userRepository,
                               @Value("${DISABLE_AUTH:false}") boolean disableAuth,
                               @Value("${ourplan.security.bypass-user-id:1}") long bypassUserId,
                               Environment environment) {
        // Fail at startup so the auth bypass can never be switched on outside the "local" profile.
        if (disableAuth && !environment.acceptsProfiles(Profiles.of("local"))) {
            throw new IllegalStateException("DISABLE_AUTH is only allowed with the local Spring profile");
        }
        this.userRepository = userRepository;
        this.disableAuth = disableAuth;
        this.bypassUserId = bypassUserId;
    }

    /** The caller's user id. Throws CalendarAccessDeniedException (403) if no OurPlan user is signed in. */
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
        if (authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        // Never treat a principal name as a user id: only an email lookup is trusted.
        String principal = authentication.getName().trim();
        return userRepository.findByEmail(principal.toLowerCase(Locale.ROOT))
                .map(User::getId)
                .orElseThrow(() -> new CalendarAccessDeniedException("Authenticated user is not an OurPlan user"));
    }

    // TEMP-AUTH-DISABLED: Keep identity-dependent features usable when the local test DB is empty.
    private Long createBypassUser() {
        return userRepository.findByEmail("test-user@ourplan.local")
                .map(User::getId)
                .orElseGet(() -> userRepository.save(new User("test-user", "test-user@ourplan.local", "")).getId());
    }
}
