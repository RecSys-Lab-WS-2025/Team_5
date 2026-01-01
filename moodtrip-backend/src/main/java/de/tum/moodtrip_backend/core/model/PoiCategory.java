package de.tum.moodtrip_backend.core.model;

public enum PoiCategory {

    NATURE("Nature"),
    HISTORY_AND_CULTURE("History & Culture"),
    ADVENTURE("Adventure"),
    RELAXATION("Relaxation"),
    FOOD_AND_CULINARY("Food & Culinary"),
    SHOPPING("Shopping");

    private final String displayName;

    public static PoiCategory fromString(String text) {
        if (text == null) return null;
        String normalized = text.toUpperCase().trim().replace(" ", "_");
        try {
            return PoiCategory.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return fromDisplayName(text);
        }
    }

    public static PoiCategory fromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (PoiCategory c : PoiCategory.values()) {
            if (c.displayName.equalsIgnoreCase(displayName)) {
                return c;
            }
        }
        return null;
    }

    PoiCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}