package com.derekgelvez.user.config;


import com.derekgelvez.user.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Locale;

/**
 * Connects Spring Security's login machinery to OurPlan accounts: how an account is looked up, how
 * passwords are hashed, and when an unverified account is rejected. The calendar service imports
 * this class too, so both services resolve a JWT's subject to the same {@code User}.
 */
@Configuration
public class ApplicationConfiguration {

    private final UserRepository userRepository;

    public ApplicationConfiguration(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Bean
    UserDetailsService userDetailsService(){
        // Accounts are identified by email everywhere: login, the JWT subject, and this lookup.
        return email -> userRepository.findByEmail(email == null ? null : email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("User not found!"));
    }

    /** BCrypt only reads the first 72 bytes of a password, hence the 72-byte limits on sign-up and reset. */
    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{
        return config.getAuthenticationManager();
    }

    /** Checks an email + password pair against the stored BCrypt hash; used by AuthenticationService.authenticate. */
    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());

        authProvider.setPasswordEncoder(passwordEncoder());
        // Check "verified?" only after the password matches, so the response to a wrong password
        // never reveals whether an email is registered or unverified.
        authProvider.setPreAuthenticationChecks(user -> { });
        authProvider.setPostAuthenticationChecks(user -> {
            if (!user.isEnabled()) {
                throw new DisabledException("Account not verified");
            }
        });

        return authProvider;

    }

}
