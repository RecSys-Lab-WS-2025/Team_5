package de.tum.moodtrip_backend.core.model;

import java.util.List;

public record Route(long relationId, String name, List<List<RouteCoordinate>> lines) {}
