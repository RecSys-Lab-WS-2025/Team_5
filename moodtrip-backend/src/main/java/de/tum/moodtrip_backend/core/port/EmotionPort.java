package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.EmotionResult;
import reactor.core.publisher.Mono;

public interface EmotionPort {
    Mono<EmotionResult> extractEmotion(String historyAndNewMessage);
}