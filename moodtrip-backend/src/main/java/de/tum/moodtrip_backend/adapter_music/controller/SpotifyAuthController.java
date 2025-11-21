package de.tum.moodtrip_backend.adapter_music.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.tum.moodtrip_backend.adapter_music.service.AuthService;
import reactor.core.publisher.Mono;

import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.util.UUID;

@RestController
public class SpotifyAuthController {

    private final AuthService authService;

    public SpotifyAuthController(AuthService authService) {
        this.authService = authService;
    }


    /**
     * 1) GET http://127.0.0.1:8080/spotify/login
     */

    @GetMapping("/spotify/login")
    public Mono<ResponseEntity<Void>> login() {
        String state = UUID.randomUUID().toString();
        String authorizeUrl = authService.buildAuthorizeUrl(state);

        return Mono.just(
                ResponseEntity.status(HttpStatus.FOUND)  // 302
                        .location(URI.create(authorizeUrl))
                        .build()
        );
    }

    /**
     * 2) GET http://127.0.0.1:8080/spotify/callback?code=...
     */
    @GetMapping("/spotify/callback")
    public Mono<String> callback(@RequestParam String code) {
        return authService.exchangeCodeForToken(code)
                .flatMap(json -> {
                    System.out.printf("✅ Auth success! Access Token: " + json.path("access_token").asText());
                    return authService.getCurrentUserProfile(json.get("access_token").asText())
                            .map(profile -> "✅ user profile:" + profile.toString());
                })
                .onErrorResume(e -> Mono.just("❌ Auth error:" + e.getMessage()));
    }


}