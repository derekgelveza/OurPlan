package com.derekgelvez.user.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final int authRequestsPerMinute;

    public SecurityConfiguration(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationProvider authenticationProvider,
            @Value("${ourplan.security.auth-requests-per-minute:10}") int authRequestsPerMinute
    ) {
        this.authenticationProvider = authenticationProvider;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authRequestsPerMinute = authRequestsPerMinute;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless token API: no cookies or sessions, so CSRF protection doesn't apply
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                // Token-only: no browser login page, Basic auth prompt or /logout endpoint
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        // /error must be reachable or failed /auth calls turn into an empty 403
                        .requestMatchers("/auth/**", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(ApiSecuritySupport.unauthorizedEntryPoint())
                        .accessDeniedHandler(ApiSecuritySupport.forbiddenHandler()))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(new AuthRateLimitFilter(authRequestsPerMinute), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Browser origins allowed to call the API (Expo web, admin pages). Native apps don't need an entry. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${ourplan.cors.allowed-origins:http://localhost:8081}") String[] allowedOrigins) {
        return ApiSecuritySupport.corsConfigurationSource(allowedOrigins);
    }
}
