package de.tum.moodtrip_backend.security;

import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod httpMethod = request.getMethod();
        String path = request.getPath().value();
        String method = httpMethod != null ? httpMethod.name() : "";

        // 1) Always let CORS preflight through
        if (HttpMethod.OPTIONS.equals(httpMethod)) {
            return chain.filter(exchange);
        }

        // 2) Public paths (no auth required)
        if (isPublicPath(path, method)) {
            return chain.filter(exchange);
        }

        // 3) Protected paths: require Bearer token
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            Long userId = jwtService.extractUserId(token);
            exchange.getAttributes().put("authUserId", userId);
            return chain.filter(exchange);
        } catch (JwtException ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String path, String method) {
        // auth endpoints (login, maybe future signup)
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        // spotify oauth callbacks
        if (path.startsWith("/api/spotify/")) {
            return true;
        }
        // user registration
        if (path.equals("/api/users") && "POST".equalsIgnoreCase(method)) {
            return true;
        }
        // actuator endpoints
        if (path.startsWith("/actuator")) {
            return true;
        }
        return false;
    }
}
