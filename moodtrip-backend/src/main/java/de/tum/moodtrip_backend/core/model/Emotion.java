package de.tum.moodtrip_backend.core.model;

public enum Emotion {
    JOYFUL,
    ENERGIZED,
    CALM,
    NEUTRAL,
    STRESSED,
    SAD,
    TIRED;
    public static Emotion fromString(String text) {
        if (text == null) return NEUTRAL;
        try {
            return Emotion.valueOf(text.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return NEUTRAL;
        }
    }
}
