package com.derekgelvez.calendar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Placeholder security setup: every endpoint requires an authenticated user.
 * Replace the authentication mechanism (e.g. a JWT resource server) here; the rest of the
 * calendar feature only depends on {@link CurrentUserProvider}.
 */
@Configuration
public class SecurityConfig {

    // TEMP-AUTH-DISABLED: Set DISABLE_AUTH=true only for local end-to-end testing.
    private final boolean disableAuth;

    public SecurityConfig(@Value("${DISABLE_AUTH:false}") boolean disableAuth) {
        this.disableAuth = disableAuth;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (disableAuth) {
            // TEMP-AUTH-DISABLED: Permit unauthenticated local requests while retaining this chain.
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .httpBasic(AbstractHttpConfigurer::disable);
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults());
        }
        return http.build();
    }
}
