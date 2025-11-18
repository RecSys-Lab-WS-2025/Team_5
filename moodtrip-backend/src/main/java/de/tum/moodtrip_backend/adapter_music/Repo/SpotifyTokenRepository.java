package de.tum.moodtrip_backend.adapter_music.Repo;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import de.tum.moodtrip_backend.adapter_music.pojo.SpotifyToken;

@Repository
public interface SpotifyTokenRepository extends ReactiveCrudRepository<SpotifyToken, String> {
}
