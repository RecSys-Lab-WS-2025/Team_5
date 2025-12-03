package de.tum.moodtrip_backend.adapter.music.spotify.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import de.tum.moodtrip_backend.core.model.SpotifyTokenDomain;
import de.tum.moodtrip_backend.core.port.SpotifyTokenPort;
import de.tum.moodtrip_backend.core.service.UserDomainService;

@Service
public class SpotifyTokenService {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyTokenService.class);
    private final SpotifyTokenPort spotifyTokenPort;
    private final UserDomainService userDomainService;

    public SpotifyTokenService(SpotifyTokenPort spotifyTokenPort, UserDomainService userDomainService) {
        this.spotifyTokenPort = spotifyTokenPort;
        this.userDomainService = userDomainService;
    }


    public Mono<SpotifyTokenDomain> create(SpotifyTokenDomain domain) {
        return spotifyTokenPort.save(domain);
    }

    public Mono<SpotifyTokenDomain> getById(Long id) {
        return spotifyTokenPort.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Spotify token not found for id: " + id
                )));
    }

    public Mono<SpotifyTokenDomain> getBySpotifyUserId(String spotifyUserId) {
        return spotifyTokenPort.findBySpotifyUserId(spotifyUserId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Spotify token not found for spotifyUserId: " + spotifyUserId
                )));
    }

    public Mono<Void> deleteById(Long id, long authUserId) {
        return spotifyTokenPort.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Spotify token not found for id: " + id
                )))
                .flatMap(token ->
                        userDomainService.findById(authUserId)
                                .flatMap(user -> {
                                    if (user.spotifyTokenId() == null ||
                                            !user.spotifyTokenId().equals(id)) {
                                        return Mono.error(new ResponseStatusException(
                                                HttpStatus.FORBIDDEN,
                                                "You are not authorized to delete this Spotify token"
                                        ));
                                    }
                                    return spotifyTokenPort.deleteById(id);
                                })
                );
    }
}
