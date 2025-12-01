package de.tum.moodtrip_backend.adapter_user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.tum.moodtrip_backend.adapter_user.dto.CreateUserRequest;
import de.tum.moodtrip_backend.adapter_user.dto.UserResponse;
import de.tum.moodtrip_backend.adapter_user.mapper.UserDtoMapper;
import de.tum.moodtrip_backend.core.service.UserDomainService;
import de.tum.moodtrip_backend.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final UserDomainService userService;
    private final UserDtoMapper mapper;
    private final JwtService jwtService;

    public UserController(UserDomainService userService, UserDtoMapper mapper, JwtService jwtService) {
        this.userService = userService;
        this.mapper = mapper;
        this.jwtService = jwtService;
    }

    @PostMapping
    public Mono<UserResponse> createUser(@Valid @RequestBody CreateUserRequest req) {
        return userService.createUser(req.username(), req.email(), req.password())
                .map(mapper::toResponse);
    }

    @GetMapping("/me")
    public Mono<UserResponse> getCurrentUser(Authentication authentication) {
        Long userId = jwtService.extractUserId(authentication);
        return userService.findById(userId)
                .map(mapper::toResponse)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
                )));
    }

    @GetMapping("/{id}")
    public Mono<UserResponse> getUserById(
            @PathVariable Long id,
            Authentication authentication) {
        
        Long authenticatedUserId = jwtService.extractUserId(authentication);
        
        if (!id.equals(authenticatedUserId)) {
            return Mono.error(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Access denied: You can only view your own profile"
            ));
        }
        
        return userService.findById(id)
                .map(mapper::toResponse)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
                )));
    }

    @GetMapping(value = "/search", params = "username")
    public Mono<UserResponse> getUserByUsername(
            @RequestParam @NotBlank String username,
            Authentication authentication) {

        return userService.findByUsername(username)
                .map(mapper::toResponse)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
                )));
    }

    @GetMapping(value = "/search", params = "email")
    public Mono<UserResponse> getUserByEmail(
            @RequestParam @NotBlank @Email String email,
            Authentication authentication) {
        
        Long authenticatedUserId = jwtService.extractUserId(authentication);
        
        return userService.findByEmail(email)
                .flatMap(user -> {
                    if (!user.id().equals(authenticatedUserId)) {
                        return Mono.error(new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Access denied: You can only search your own email"
                        ));
                    }
                    return Mono.just(mapper.toResponse(user));
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
                )));
    }

    @DeleteMapping("/delete/{id}")
    public Mono<Void> deleteUser(
            @PathVariable Long id,
            Authentication authentication) {
        
        Long authenticatedUserId = jwtService.extractUserId(authentication);
        
        if (!id.equals(authenticatedUserId)) {
            return Mono.error(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Access denied: You can only delete your own account"
            ));
        }
        
        return userService.deleteUser(id);
    }
}
