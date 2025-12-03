package de.tum.moodtrip_backend.core.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record SurveyDomain(
        Long id,
        Long userId,
        Long conversationId,
        Double latitude,
        Double longitude,
        Integer rangeMeters,
        LocalDate startDate,
        LocalDate endDate,
        List<PoiCategory> poiCategories,
        LocalDateTime createdAt
) {
    public SurveyDomain {
        poiCategories = poiCategories == null ? Collections.emptyList() : Collections.unmodifiableList(poiCategories);
    }
}
