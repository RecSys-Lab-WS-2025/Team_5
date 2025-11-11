package de.tum.moodtrip_backend.adapter_music.Repo;

import de.tum.moodtrip_backend.adapter_music.pojo.SpotifyToken;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpotifyTokenRepository extends ReactiveCrudRepository<SpotifyToken, String> {
}
