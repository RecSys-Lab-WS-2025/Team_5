package de.tum.moodtrip_backend.security;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwtService;

    public JwtAuthenticationManager(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.just(authentication)
                .cast(JwtToken.class)
                .filter(jwt -> jwtService.isTokenValid(jwt.getClaims()))
                .map(jwt -> jwt.withAuthenticated(true))
                .cast(Authentication.class)
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid JWT token")));
    }
}