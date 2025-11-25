package de.tum.moodtrip_backend.adapter_user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.tum.moodtrip_backend.adapter_user.dto.LoginRequest;
import de.tum.moodtrip_backend.adapter_user.dto.LoginResponse;
import de.tum.moodtrip_backend.core.service.UserDomainService;
import de.tum.moodtrip_backend.exception.UserNotFoundException;

import jakarta.validation.Valid;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final UserDomainService userService;

    public AuthController(UserDomainService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
        return userService.findByEmail(req.email())
                .flatMap(user -> userService.authenticate(user, req.password())
                        .map(ResponseEntity::ok)
                        .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()))
                )
                .onErrorResume(UserNotFoundException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
                );
    }

}
