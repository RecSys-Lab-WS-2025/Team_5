package de.tum.moodtrip_backend.core.model;

public enum PoiCategory {

    NATURE("Nature"),
    HISTORY_AND_CULTURE("History & Culture"),
    ADVENTURE("Adventure"),
    RELAXATION("Relaxation"),
    FOOD_AND_CULINARY("Food & Culinary"),
    NIGHTLIFE("Nightlife"),
    ART_AND_MUSEUMS("Art & Museums"),
    SHOPPING("Shopping"),
    BEACH_AND_COAST("Beach & Coast"),
    CITY_EXPLORATION("City Exploration");

    private final String displayName;

    public static PoiCategory fromDisplayName(String displayName) {
        if (displayName == null) {
            throw new IllegalArgumentException("displayName must not be null");
        }
        for (PoiCategory c : PoiCategory.values()) {
            if (c.displayName.equalsIgnoreCase(displayName)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown POI category display name: " + displayName);
    }

    PoiCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}