package de.tum.moodtrip_backend.core.model;

/**
 * Defines the different route generation strategies.
 */
public enum RouteType {
    BALANCED("Balanced Route", "Classic balanced mood and interest route"),
    YOUR_PICKS("Your Picks", "Route focused on your selected categories"),
    DISCOVERY("Try Something New", "Diverse route to explore new experiences");

    private final String displayTitle;
    private final String description;

    RouteType(String displayTitle, String description) {
        this.displayTitle = displayTitle;
        this.description = description;
    }

    public String getDisplayTitle() {
        return displayTitle;
    }

    public String getDescription() {
        return description;
    }
}
