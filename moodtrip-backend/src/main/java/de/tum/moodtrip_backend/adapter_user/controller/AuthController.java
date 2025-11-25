package de.tum.moodtrip_backend.adapter_user.controller;

import java.util.UUID;

import de.tum.moodtrip_backend.adapter_user.dto.LoginRequest;
import de.tum.moodtrip_backend.adapter_user.dto.LoginResponse;
import de.tum.moodtrip_backend.core.model.UserProfile;
import de.tum.moodtrip_backend.core.service.UserDomainService;
import de.tum.moodtrip_backend.exception.UserNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final UserDomainService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserDomainService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
        return userService.findByEmail(req.email())
                .flatMap(user -> authenticate(user, req.password()))
                .onErrorResume(UserNotFoundException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
                );
    }

    private Mono<ResponseEntity<LoginResponse>> authenticate(UserProfile user, String rawPassword) {
        String hash = user.passwordHash();
        if (hash == null || !passwordEncoder.matches(rawPassword, hash)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String token = UUID.randomUUID().toString();

        LoginResponse response = new LoginResponse(
                token,
                new LoginResponse.UserDto(
                        user.id(),
                        user.username(),
                        user.email()
                )
        );

        return Mono.just(ResponseEntity.ok(response));
}

}
