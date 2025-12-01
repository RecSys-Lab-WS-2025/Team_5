package de.tum.moodtrip_backend.core.model;

public enum SurveyPreference {
    NATURAL_LANDSCAPE,
    CULTURAL,
    MODERN,
    HISTORICAL;

    public static SurveyPreference fromString(String value) {
        if (value == null) {
            return null;
        }
        for (SurveyPreference pref : SurveyPreference.values()) {
            if (pref.name().equalsIgnoreCase(value)) {
                return pref;
            }
        }
        throw new IllegalArgumentException("Unknown preference: " + value);
    }
}
