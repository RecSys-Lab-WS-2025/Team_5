package de.tum.moodtrip_backend.core.model;

public enum Emotion {
    JOYFUL,
    ENERGIZED,
    CALM,
    CONTENT,
    HOPEFUL,
    GRATEFUL,
    CURIOUS,
    NOSTALGIC,
    NEUTRAL,
    CONFUSED,
    BORED,
    TIRED,
    LONELY,
    SAD,
    ANXIOUS,
    STRESSED,
    FRUSTRATED,
    ANGRY,
    OVERWHELMED;
    public static Emotion fromString(String text) {
        if (text == null) return NEUTRAL;
        try {
            return Emotion.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NEUTRAL;
        }
    }
}
