package de.tum.moodtrip_backend.adapter_music.controller;

import de.tum.moodtrip_backend.adapter_music.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class SpotifyAuthController {

    private final AuthService authService;

    public SpotifyAuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/spotify/callback")
    public Mono<String> callback(@RequestParam String code) {
        return authService.exchangeCodeForToken(code)
                .flatMap(json -> {
                    System.out.printf("✅ Auth success！Access Token: " + json.path("access_token").asText());
                    return authService.getCurrentUserProfile(json.get("access_token").asText())
                            .map(profile -> "✅ user profile:" + profile.toString());
                })
                .onErrorResume(e -> Mono.just("❌ Auth error:" + e.getMessage()));
    }
}