package de.tum.moodtrip_backend.security;

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
                .<Authentication>flatMap(token -> Mono.defer(() -> Mono.just(createAuthentication(token))))
                .onErrorResume(JwtException.class, e -> Mono.empty());
    }

    private JwtToken createAuthentication(String token) {
        Long userId = jwtService.extractUserId(token);
        String username = jwtService.extractUsername(token);

        UserDetails userDetails = User.withUsername(username)
                .password("") // not used
                .authorities(Collections.emptyList()) // no roles yet
                .build();

        return new JwtToken(token, userId, userDetails);
    }
}