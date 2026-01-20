package de.tum.moodtrip_backend.core.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import de.tum.moodtrip_backend.api.dto.LoginResponse;
import de.tum.moodtrip_backend.core.model.UserProfile;
import de.tum.moodtrip_backend.core.port.UserPort;
import de.tum.moodtrip_backend.exception.UserNotFoundException;
import de.tum.moodtrip_backend.api.security.JwtService;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserDomainService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDomainService.class);

    private final UserPort userPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;


    public UserDomainService(UserPort userPort, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userPort = userPort;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Mono<UserProfile> createUser(String username, String email) {
        return createUser(username, email, null);
    }

    public Mono<UserProfile> createUser(String username, String email, String rawPassword) {
        LOGGER.info("Attempting to create user with username: {}", username);
        return userPort.existsByUsername(username)
                .flatMap(usernameExists -> {
                    if (usernameExists) {
                        LOGGER.warn("Username already exists: {}", username);
                        return Mono.error(new IllegalArgumentException("Username already exists: " + username));
                    }
                    return userPort.existsByEmail(email)
                            .flatMap(emailExists -> {
                                if (emailExists) {
                                    LOGGER.warn("Email already exists: {}", email);
                                    return Mono.error(new IllegalArgumentException("Email already exists: " + email));
                                }

                                String hash = rawPassword != null ? passwordEncoder.encode(rawPassword) : null;

                                UserProfile user = new UserProfile(
                                        null,
                                        username,
                                        email,
                                        LocalDateTime.now(),
                                        hash,
                                        null,
                                        null
                                );
                                return userPort.save(user)
                                        .doOnSuccess(u -> LOGGER.info("User created successfully: {}", u.id()));
                            });
                });
    }

    public Mono<UserProfile> findById(Long id) {
        return userPort.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with Id: " + id)));
    }

    public Mono<UserProfile> findByUsername(String username) {
        return userPort.findByUsername(username)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with username: " + username)));
    }

    public Mono<UserProfile> findByEmail(String email) {
        return userPort.findByEmail(email)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with email: " + email)));
    }

    public Mono<Void> deleteUser(Long id) {
        LOGGER.info("Deleting user with ID: {}", id);
        return userPort.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with Id: " + id)))
                .then(userPort.deleteById(id));
    }
  
    /**
     * Create or link user profile from Spotify OAuth
     */
    public Mono<UserProfile> updateUsername(Long userId, String newUsername) {
        String username = newUsername == null ? "" : newUsername.trim();

        if (username.isBlank()) {
            return Mono.error(new IllegalArgumentException("Username cannot be blank"));
        }
        if (username.length() < 2 || username.length() > 50) {
            return Mono.error(new IllegalArgumentException("Username must be between 2 and 50 characters"));
        }

        LOGGER.info("Updating username for user {} -> {}", userId, username);

        return userPort.existsByUsername(username)
                .flatMap(exists -> {
                    if (exists) {
                        return userPort.findById(userId)
                                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with Id: " + userId)))
                                .flatMap(user -> {
                                    if (username.equals(user.username())) {
                                        return Mono.just(user);
                                    }
                                    return Mono.error(new IllegalArgumentException("Username already exists: " + username));
                                });
                    }

                    return userPort.findById(userId)
                            .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with Id: " + userId)))
                            .flatMap(user -> {
                                UserProfile updated = new UserProfile(
                                        user.id(),
                                        username,
                                        user.email(),
                                        user.createdAt(),
                                        user.passwordHash(),
                                        user.spotifyTokenId(),
                                        user.avatarUrl()
                                );
                                return userPort.save(updated)
                                        .doOnSuccess(u -> LOGGER.info("Username updated successfully for user {}", u.id()));
                            });
                });
    }

    public Mono<UserProfile> updateAvatarUrl(Long userId, String avatarUrl) {
        String url = avatarUrl == null ? null : avatarUrl.trim();

        LOGGER.info("Updating avatarUrl for user {} -> {}", userId, url);

        return userPort.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with Id: " + userId)))
                .flatMap(user -> {
                    UserProfile updated = new UserProfile(
                            user.id(),
                            user.username(),
                            user.email(),
                            user.createdAt(),
                            user.passwordHash(),
                            user.spotifyTokenId(),
                            url
                    );
                    return userPort.save(updated)
                            .doOnSuccess(u -> LOGGER.info("Avatar updated successfully for user {}", u.id()));
                });
    }

    public Mono<UserProfile> createOrLinkSpotifyUser(Long spotifyTokenId, String spotifyEmail, String spotifyDisplayName) {
        LOGGER.info("Linking/Creating user from Spotify. Email: {}", spotifyEmail);
        // 1. Check if already linked
        return userPort.findBySpotifyTokenId(spotifyTokenId)
                .doOnNext(u -> LOGGER.info("User already linked to token {}: {}", spotifyTokenId, u.username()))
                .switchIfEmpty(Mono.defer(() -> {
                // 2. Try to link to existing user by email
                    if (spotifyEmail != null && !spotifyEmail.isBlank()) {
                        return userPort.findByEmail(spotifyEmail)
                                .doOnNext(u -> LOGGER.info("Found existing user by email {}, linking spotify account", spotifyEmail))
                                .flatMap(existingUser -> linkSpotifyAccount(existingUser.id(), spotifyTokenId))
                                .switchIfEmpty(Mono.defer(() -> createUserFromSpotify(spotifyTokenId, spotifyEmail, spotifyDisplayName)));
                    }
                    // 3. Create new user
                    return createUserFromSpotify(spotifyTokenId, spotifyEmail, spotifyDisplayName);
                }));
    }

    private Mono<UserProfile> linkSpotifyAccount(Long userId, Long spotifyTokenId) {
        return userPort.findById(userId)
                .flatMap(user -> {
                    UserProfile updated = new UserProfile(
                            user.id(),
                            user.username(),
                            user.email(),
                            user.createdAt(),
                            user.passwordHash(),
                            spotifyTokenId,
                            user.avatarUrl()
                    );
                    return userPort.save(updated)
                            .doOnSuccess(u -> LOGGER.info("Linked Spotify token {} to user {}", spotifyTokenId, u.id()));
                });
    }

    private Mono<UserProfile> createUserFromSpotify(Long spotifyTokenId, String spotifyEmail, String spotifyDisplayName) {
        String username = generateUsernameFromSpotify(spotifyEmail, spotifyDisplayName);
        LOGGER.info("Creating new Spotify user with username: {}", username);

        UserProfile newUser = new UserProfile(
                null,
                username,
                spotifyEmail,
                LocalDateTime.now(),
                null,
                spotifyTokenId,
                null
        );

        return userPort.save(newUser)
                .doOnSuccess(u -> LOGGER.info("Created new Spotify user {}", u.id()));
    }

    private String generateUsernameFromSpotify(String spotifyEmail, String spotifyDisplayName) {
        if (spotifyDisplayName != null && !spotifyDisplayName.isBlank() && spotifyDisplayName.matches("[a-zA-Z0-9_-]+")) {
            return spotifyDisplayName;
        }

        if (spotifyEmail != null && !spotifyEmail.isBlank() && spotifyEmail.contains("@")) {
            String emailPrefix = spotifyEmail.split("@")[0];
            return sanitizeUsername(emailPrefix);
        }

        if (spotifyDisplayName != null && !spotifyDisplayName.isBlank()) {
            return sanitizeUsername(spotifyDisplayName);
        }

        return "spotify_user_" + System.currentTimeMillis();
    }

    private String sanitizeUsername(String displayName) {
        return displayName.replaceAll("[^a-zA-Z0-9_-]", "_")
                .substring(0, Math.min(50, displayName.length()));
    }

    public Mono<LoginResponse> authenticate(UserProfile user, String rawPassword) {
        String hash = user.passwordHash();
        if (hash == null || !passwordEncoder.matches(rawPassword, hash)) {
            return Mono.empty();
        }

        String token = jwtService.generateToken(user);

        LoginResponse response = new LoginResponse(
                token,
                new LoginResponse.UserDto(
                        user.id(),
                        user.username(),
                        user.email()
                )
        );

        return Mono.just(response);
    }
}
