package de.tum.moodtrip_backend.adapter_user.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import de.tum.moodtrip_backend.adapter_user.dto.CreateUserRequest;
import de.tum.moodtrip_backend.adapter_user.dto.UserResponse;
import de.tum.moodtrip_backend.adapter_user.mapper.UserDtoMapper;
import de.tum.moodtrip_backend.core.service.UserDomainService;
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

    public UserController(UserDomainService userService, UserDtoMapper mapper) {
        this.userService = userService;
        this.mapper = mapper;
    }

    @PostMapping
    public Mono<UserResponse> createUser(@Valid @RequestBody CreateUserRequest req) {
        return userService.createUser(req.username(), req.email())
                .map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    public Mono<UserResponse> getUserById(@PathVariable Long id) {
        return userService.findById(id).map(mapper::toResponse);
    }

    @GetMapping(value ="/search",params = "username")
    public Mono<UserResponse> getUserByUsername(@RequestParam @NotBlank String username) {
        return userService.findByUsername(username).map(mapper::toResponse);
    }

    @GetMapping(value = "/search",params = "email")
    public Mono<UserResponse> getUserByEmail(@RequestParam @NotBlank @Email String email) {
        return userService.findByEmail(email).map(mapper::toResponse);
    }

    @DeleteMapping("/delete/{id}")
    public Mono<Void> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }
}
