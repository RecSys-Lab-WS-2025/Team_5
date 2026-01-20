package de.tum.moodtrip_backend.api.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import reactor.core.publisher.Mono;

import de.tum.moodtrip_backend.api.dto.CreateUserRequest;
import de.tum.moodtrip_backend.api.dto.UserResponse;
import de.tum.moodtrip_backend.api.mapper.UserDtoMapper;
import de.tum.moodtrip_backend.api.security.JwtService;
import de.tum.moodtrip_backend.core.service.UserDomainService;

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

    public record UpdateProfileRequest(
            @NotBlank
            @Size(min = 2, max = 50)
            String username,
            String avatarUrl
    ) {}

    public record UploadAvatarResponse(String avatarUrl) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
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

    @PatchMapping("/me/profile")
    public Mono<UserResponse> patchMyProfile(
            @Valid @RequestBody UpdateProfileRequest req,
            Authentication authentication
    ) {
        return updateMyProfileInternal(req, authentication);
    }

    @PutMapping("/me/profile")
    public Mono<UserResponse> putMyProfile(
            @Valid @RequestBody UpdateProfileRequest req,
            Authentication authentication
    ) {
        return updateMyProfileInternal(req, authentication);
    }

    private Mono<UserResponse> updateMyProfileInternal(
            UpdateProfileRequest req,
            Authentication authentication
    ) {
        Long userId = jwtService.extractUserId(authentication);

        Mono<Void> usernameUpdate = userService.updateUsername(userId, req.username()).then();
        Mono<Void> avatarUpdate = Mono.empty();

        if (req.avatarUrl() != null) {
            avatarUpdate = userService.updateAvatarUrl(userId, req.avatarUrl()).then();
        }

        return usernameUpdate
                .then(avatarUpdate)
                .then(userService.findById(userId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .map(mapper::toResponse);
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<UploadAvatarResponse> uploadMyAvatar(
            @RequestPart("file") Mono<FilePart> file,
            Authentication authentication
    ) {
        Long userId = jwtService.extractUserId(authentication);

        return file.flatMap(fp -> {
            String original = fp.filename() == null ? "" : fp.filename();
            String ext = "";

            int dot = original.lastIndexOf('.');
            if (dot >= 0 && dot < original.length() - 1) {
                ext = original.substring(dot).toLowerCase();
                if (!ext.matches("\\.(png|jpg|jpeg|webp)")) {
                    ext = "";
                }
            }

            String safeName = "avatar-" + UUID.randomUUID() + ext;

            Path dir = Paths.get("uploads", "avatars", String.valueOf(userId));
            Path target = dir.resolve(safeName);

            Mono<Void> ensureDir = Mono.fromRunnable(() -> {
                try {
                    Files.createDirectories(dir);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Mono<Void> saveFile = ensureDir.then(fp.transferTo(target));

            String avatarUrl = "/uploads/avatars/" + userId + "/" + safeName;

            return saveFile
                    .then(userService.updateAvatarUrl(userId, avatarUrl))
                    .thenReturn(new UploadAvatarResponse(avatarUrl));
        });
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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
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
