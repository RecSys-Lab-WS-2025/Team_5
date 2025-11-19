package de.tum.moodtrip_backend.core.model;

import java.util.Map;

public record EmotionResult(
        Map<Emotion, Double> scores,
        Emotion topLabel,
        double topScore,
        String content,
        boolean success
) {
}