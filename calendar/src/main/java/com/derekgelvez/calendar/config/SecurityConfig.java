package com.derekgelvez.calendar.config;

import com.derekgelvez.user.config.ApiSecuritySupport;
import com.derekgelvez.user.config.JwtAuthenticationFilter;
import com.derekgelvez.user.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Every calendar endpoint requires a valid JWT issued by the user app
 * ({@code Authorization: Bearer <token>}). The token is checked with the same JwtService,
 * secret and filter as the user app, so both apps agree on who is signed in.
 * The rest of the calendar feature only depends on {@link CurrentUserProvider}.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    // TEMP-AUTH-DISABLED: Set DISABLE_AUTH=true only for local end-to-end testing.
    private final boolean disableAuth;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(@Value("${DISABLE_AUTH:false}") boolean disableAuth,
                          JwtService jwtService,
                          UserDetailsService userDetailsService,
                          Environment environment) {
        if (disableAuth && !environment.acceptsProfiles(Profiles.of("local"))) {
            throw new IllegalStateException("DISABLE_AUTH is only allowed with the local Spring profile");
        }
        this.disableAuth = disableAuth;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                // Token-only: no Basic auth prompt, login page or /logout endpoint
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(ApiSecuritySupport.unauthorizedEntryPoint())
                        .accessDeniedHandler(ApiSecuritySupport.forbiddenHandler()));

        if (disableAuth) {
            // TEMP-AUTH-DISABLED: Permit unauthenticated local requests while retaining this chain.
            log.warn("DISABLE_AUTH=true: authentication AND calendar permission checks are OFF. Never use this outside local testing.");
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        } else {
            http.authorizeHttpRequests(auth -> auth
                            .requestMatchers("/error").permitAll()
                            .anyRequest().authenticated())
                    .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                            UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }

    /** Browser origins allowed to call the API (Expo web). Native apps don't need an entry. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${ourplan.cors.allowed-origins:http://localhost:8081}") String[] allowedOrigins) {
        return ApiSecuritySupport.corsConfigurationSource(allowedOrigins);
    }
}
