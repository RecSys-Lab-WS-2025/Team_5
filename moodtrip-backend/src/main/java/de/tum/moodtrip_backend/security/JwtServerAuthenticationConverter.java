package de.tum.moodtrip_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
public class JwtServerAuthenticationConverter implements ServerAuthenticationConverter {

    private final JwtService jwtService;
    private static final String BEARER_PREFIX = "Bearer ";

    public JwtServerAuthenticationConverter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(header -> header.startsWith(BEARER_PREFIX))
                .map(header -> header.substring(BEARER_PREFIX.length()))
                .flatMap(this::createAuthentication);
    }

    private Mono<Authentication> createAuthentication(String token) {
        try {
            // Parse the token only once and reuse the claims
            Claims claims = jwtService.parseToken(token);
            Long userId = jwtService.extractUserId(claims);
            String username = jwtService.extractUsername(claims);

            UserDetails userDetails = User.withUsername(username)
                    .password("") // not used
                    .authorities(Collections.emptyList()) // no roles yet
                    .build();

            return Mono.just(new JwtToken(token, userId, userDetails, claims));
        } catch (JwtException e) {
            // Return empty to indicate authentication conversion failed gracefully
            return Mono.empty();
        }
    }
}