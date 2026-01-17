package de.tum.moodtrip_backend.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;

import de.tum.moodtrip_backend.api.security.JwtAuthenticationManager;
import de.tum.moodtrip_backend.api.security.JwtServerAuthenticationConverter;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;

import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationManager authenticationManager;
    private final JwtServerAuthenticationConverter authenticationConverter;
    private final org.springframework.web.cors.reactive.CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            JwtAuthenticationManager authenticationManager,
            JwtServerAuthenticationConverter authenticationConverter,
            org.springframework.web.cors.reactive.CorsConfigurationSource corsConfigurationSource
    ) {
        this.authenticationManager = authenticationManager;
        this.authenticationConverter = authenticationConverter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        AuthenticationWebFilter authWebFilter = new AuthenticationWebFilter(authenticationManager);
        authWebFilter.setServerAuthenticationConverter(authenticationConverter);

        authWebFilter.setRequiresAuthenticationMatcher(
                new NegatedServerWebExchangeMatcher(
                        pathMatchers(
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/users",
                                "/api/spotify/login",
                                "/api/spotify/callback",
                                "/actuator/**",
                                "/uploads/**"
                        )
                )
        );

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .pathMatchers("/api/auth/login").permitAll()
                        .pathMatchers("/api/auth/refresh").permitAll()
                        .pathMatchers("/api/spotify/login").permitAll()
                        .pathMatchers("/api/spotify/callback").permitAll()
                        .pathMatchers("/actuator/**").permitAll()

                        .pathMatchers("/uploads/**").permitAll()

                        .anyExchange().authenticated()
                )
                .addFilterAt(authWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }
}
