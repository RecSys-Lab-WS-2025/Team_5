package de.tum.moodtrip_backend.adapter_database.adapter;

import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.adapter_database.mapper.UserMapper;
import de.tum.moodtrip_backend.adapter_database.repository.R2dbcUserRepository;
import de.tum.moodtrip_backend.core.model.UserProfile;
import de.tum.moodtrip_backend.core.port.UserPort;
import reactor.core.publisher.Mono;

@Component
public class DatabaseUserAdapter implements UserPort {

    private final R2dbcUserRepository userRepository;
    private final UserMapper userMapper;

    public DatabaseUserAdapter(R2dbcUserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Mono<UserProfile> save(UserProfile user) {
        return Mono.just(user)
                .map(userMapper::toEntity)
                .flatMap(userRepository::save)
                .map(userMapper::toDomain);
    }

    @Override
    public Mono<UserProfile> findById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDomain);
    }

    @Override
    public Mono<UserProfile> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toDomain);
    }

    @Override
    public Mono<UserProfile> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return userRepository.deleteById(id);
    }
}
