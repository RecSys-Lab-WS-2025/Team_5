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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
@Validated
@Tag(name = "User Management", description = "APIs for managing user accounts and profiles")
public class UserController {

    private final UserDomainService userService;
    private final UserDtoMapper mapper;
    private final JwtService jwtService;

    public UserController(UserDomainService userService, UserDtoMapper mapper, JwtService jwtService) {
        this.userService = userService;
        this.mapper = mapper;
        this.jwtService = jwtService;
    }

    @Operation(
        summary = "Create a new user account",
        description = "Registers a new user with username, email, and password"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User created successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        )
    })
    @PostMapping
    public Mono<UserResponse> createUser(@Valid @RequestBody CreateUserRequest req) {
        return userService.createUser(req.username(), req.email(), req.password())
                .map(mapper::toResponse);
    }

    @Operation(
        summary = "Get current authenticated user",
        description = "Retrieves the profile information of the currently authenticated user",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User profile retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        )
    })
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

    @Operation(
        summary = "Get user by ID",
        description = "Retrieves user profile by user ID. Users can only access their own profile.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User profile retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        )
    })
    @GetMapping("/{id}")
    public Mono<UserResponse> getUserById(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
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

    @Operation(
        summary = "Search user by username",
        description = "Finds a user by their username",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        )
    })
    @GetMapping(value = "/search", params = "username")
    public Mono<UserResponse> getUserByUsername(
            @Parameter(description = "Username to search for", required = true) @RequestParam @NotBlank String username,
            Authentication authentication) {

        return userService.findByUsername(username)
                .map(mapper::toResponse)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
                )));
    }

    @Operation(
        summary = "Search user by email",
        description = "Finds a user by their email address. Users can only search their own email.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        )
    })
    @GetMapping(value = "/search", params = "email")
    public Mono<UserResponse> getUserByEmail(
            @Parameter(description = "Email address to search for", required = true) @RequestParam @NotBlank @Email String email,
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

    @Operation(
        summary = "Delete user account",
        description = "Deletes a user account. Users can only delete their own account.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User account deleted successfully"
        )
    })
    @DeleteMapping("/delete/{id}")
    public Mono<Void> deleteUser(
            @Parameter(description = "User ID to delete", required = true) @PathVariable Long id,
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
