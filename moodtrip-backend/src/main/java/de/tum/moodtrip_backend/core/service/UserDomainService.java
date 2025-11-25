package de.tum.moodtrip_backend.core.service;

import java.time.LocalDateTime;

import de.tum.moodtrip_backend.exception.UserNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import de.tum.moodtrip_backend.core.model.UserProfile;
import de.tum.moodtrip_backend.core.port.UserPort;
import reactor.core.publisher.Mono;

@Service
public class UserDomainService {

    private final UserPort userPort;
    private final PasswordEncoder passwordEncoder;

    public UserDomainService(UserPort userPort, PasswordEncoder passwordEncoder) {
        this.userPort = userPort;
        this.passwordEncoder = passwordEncoder;
    }

    public Mono<UserProfile> createUser(String username, String email) {
        return createUser(username, email, null);
    }

    public Mono<UserProfile> createUser(String username, String email, String rawPassword) {
        return userPort.existsByUsername(username)
                .flatMap(usernameExists -> {
                    if (usernameExists) {
                        return Mono.error(new IllegalArgumentException("Username already exists: " + username));
                    }
                    return userPort.existsByEmail(email)
                            .flatMap(emailExists -> {
                                if (emailExists) {
                                    return Mono.error(new IllegalArgumentException("Email already exists: " + email));
                                }

                                String hash = rawPassword != null ? passwordEncoder.encode(rawPassword) : null;

                                UserProfile user = new UserProfile(
                                        null,
                                        username,
                                        email,
                                        LocalDateTime.now(),
                                        hash
                                );
                                return userPort.save(user);
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
        return userPort.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with Id: " + id)))
                .then(userPort.deleteById(id));
    }
}
