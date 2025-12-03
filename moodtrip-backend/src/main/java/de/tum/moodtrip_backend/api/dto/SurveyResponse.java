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
        List<String> preferences,
        LocalDateTime createdAt
) {
    public SurveyResponse {
        preferences = preferences == null ? Collections.emptyList() : Collections.unmodifiableList(preferences);
    }
}
