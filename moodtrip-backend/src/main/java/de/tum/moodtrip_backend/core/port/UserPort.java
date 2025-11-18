package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.UserProfile;
import reactor.core.publisher.Mono;

public interface UserPort {
    Mono<UserProfile> save(UserProfile user);
    Mono<UserProfile> findById(Long id);
    Mono<UserProfile> findByUsername(String username);
    Mono<UserProfile> findByEmail(String email);
    Mono<Boolean> existsByUsername(String username);
    Mono<Boolean> existsByEmail(String email);
    Mono<Void> deleteById(Long id);
}
