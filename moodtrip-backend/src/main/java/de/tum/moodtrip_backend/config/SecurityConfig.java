package de.tum.moodtrip_backend.config;

import de.tum.moodtrip_backend.security.JwtAuthenticationManager;
import de.tum.moodtrip_backend.security.JwtServerAuthenticationConverter;
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

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationManager authenticationManager;
    private final JwtServerAuthenticationConverter authenticationConverter;

    public SecurityConfig(JwtAuthenticationManager authenticationManager, JwtServerAuthenticationConverter authenticationConverter) {
        this.authenticationManager = authenticationManager;
        this.authenticationConverter = authenticationConverter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        AuthenticationWebFilter authWebFilter = new AuthenticationWebFilter(authenticationManager);
        authWebFilter.setServerAuthenticationConverter(authenticationConverter);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .pathMatchers("/api/auth/login").permitAll()
                        .pathMatchers("/api/spotify/login").permitAll()
                        .pathMatchers("/api/spotify/callback").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(authWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }
}
