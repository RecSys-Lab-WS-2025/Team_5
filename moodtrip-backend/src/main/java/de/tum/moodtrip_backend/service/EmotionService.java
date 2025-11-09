package de.tum.moodtrip_backend.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class EmotionService {
    public Mono<String> analyzeEmotion(String text) {
        // Placeholder for emotion analysis logic
        return Mono.just("Detected emotion: Happy!");// TODO: Replace with actual emotion analysis implementation
    }
}
