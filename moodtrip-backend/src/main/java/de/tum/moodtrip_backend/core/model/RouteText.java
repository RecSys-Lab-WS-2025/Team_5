package de.tum.moodtrip_backend.core.model;

import java.util.Map;

public record RouteText(String title, Map<Integer, String> dayDescriptions) {
}