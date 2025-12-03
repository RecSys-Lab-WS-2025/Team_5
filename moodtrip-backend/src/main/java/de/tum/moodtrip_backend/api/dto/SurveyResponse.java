package de.tum.moodtrip_backend.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record SurveyResponse(
        Long id,
        Long userId,
        Long conversationId,
        String location,
        Integer rangeMeters,
        LocalDate startDate,
        LocalDate endDate,
        List<String> poiCategories,
        LocalDateTime createdAt
) {
    public SurveyResponse {
        poiCategories = poiCategories == null ? Collections.emptyList() : Collections.unmodifiableList(poiCategories);
    }
}
