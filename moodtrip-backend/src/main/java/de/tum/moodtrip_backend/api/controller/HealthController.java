package de.tum.moodtrip_backend.api.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HealthController {
    @GetMapping("/custom-health")
    public Mono<String> healthCheck() {
        return Mono.just("backend is running");
    }
}
