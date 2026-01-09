package de.tum.moodtrip_backend.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.tum.moodtrip_backend.api.dto.LoginRequest;
import de.tum.moodtrip_backend.api.dto.LoginResponse;
import de.tum.moodtrip_backend.api.mapper.UserDtoMapper;
import de.tum.moodtrip_backend.api.security.JwtService;
import de.tum.moodtrip_backend.core.service.UserDomainService;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final UserDomainService userService;
    private final JwtService jwtService;

    public AuthController(UserDomainService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public Mono<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return userService.findByEmail(req.email())
                .flatMap(user -> userService.authenticate(user, req.password()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")));
    }

    @PostMapping("/refresh")
    public Mono<LoginResponse> refresh(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return Mono.justOrEmpty(authHeader)
            .filter(header -> header.startsWith("Bearer "))
            .map(header -> header.substring(7))
            .switchIfEmpty(Mono.error(new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, 
                "Missing or invalid Authorization header"
            )))
            .flatMap(token -> 
                Mono.fromCallable(() -> jwtService.refreshCurrentToken(token))
                    .subscribeOn(Schedulers.boundedElastic()) 
                    .flatMap(newToken -> {
                        Long userId = jwtService.extractUserId(newToken);
                        String username = jwtService.extractUsername(newToken);
                        String email = jwtService.extractEmail(newToken);
                        LOGGER.info("Successfully refreshed token for user ID: {}", userId);
                        LoginResponse.UserDto userDto = new LoginResponse.UserDto(userId, username, email);
                        return Mono.just(new LoginResponse(newToken, userDto));
                    })
            )
            .onErrorResume(e -> {
                if (e instanceof ResponseStatusException) {
                    return Mono.error(e);
                }
                LOGGER.error("Token refresh failed: {}", e.getMessage(), e);
                return Mono.error(new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, 
                    "Invalid or expired token"
                ));
            });
    }
}
