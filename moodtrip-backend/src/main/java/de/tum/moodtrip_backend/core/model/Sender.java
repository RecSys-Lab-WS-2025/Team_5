package de.tum.moodtrip_backend.core.model;

public enum Sender {
    USER,
    BOT;
    public static Sender fromString(String text) {
        return Sender.valueOf(text.toUpperCase());
    }
}
