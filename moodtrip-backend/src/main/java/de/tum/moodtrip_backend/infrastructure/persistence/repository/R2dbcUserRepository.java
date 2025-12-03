package de.tum.moodtrip_backend.infrastructure.persistence.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.UserEntity;
import reactor.core.publisher.Mono;

@Repository
public interface R2dbcUserRepository extends ReactiveCrudRepository<UserEntity, Long> {
    @Query("SELECT * FROM user_profile WHERE username = :username")
    Mono<UserEntity> findByUsername(String username);
    
    @Query("SELECT * FROM user_profile WHERE email = :email")
    Mono<UserEntity> findByEmail(String email);
    
    @Query("SELECT * FROM user_profile WHERE spotify_token_id = :spotifyTokenId")
    Mono<UserEntity> findBySpotifyTokenId(Long spotifyTokenId);
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_profile WHERE username = :username)")
    Mono<Boolean> existsByUsername(String username);
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_profile WHERE email = :email)")
    Mono<Boolean> existsByEmail(String email);
}
