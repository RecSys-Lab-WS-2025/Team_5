package de.tum.moodtrip_backend.adapter_database.mapper;

import de.tum.moodtrip_backend.adapter_database.entity.SurveyEntity;
import de.tum.moodtrip_backend.core.model.SurveyDomain;
import de.tum.moodtrip_backend.core.model.SurveyPreference;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class SurveyMapper {

    public SurveyDomain toDomain(final SurveyEntity entity) {
        if (entity == null) {
            return null;
        }
        
        List<SurveyPreference> preferences = parsePreferences(entity.getPreferences());
        
        return new SurveyDomain(
                entity.getId(),
                entity.getUserId(),
                entity.getConversationId(),
                entity.getLocation(),
                entity.getRangeMeters(),
                entity.getStartDate(),
                entity.getEndDate(),
                preferences,
                entity.getCreatedAt()
        );
    }

    public SurveyEntity toEntity(final SurveyDomain domain) {
        if (domain == null) {
            return null;
        }
        
        String preferencesStr = serializePreferences(domain.preferences());
        LocalDateTime createdAt = domain.createdAt() != null ? domain.createdAt() : LocalDateTime.now();
        
        return new SurveyEntity(
                domain.id(),
                domain.userId(),
                domain.conversationId(),
                domain.location(),
                domain.rangeMeters(),
                domain.startDate(),
                domain.endDate(),
                preferencesStr,
                createdAt
        );
    }

    private List<SurveyPreference> parsePreferences(String preferencesStr) {
        if (preferencesStr == null || preferencesStr.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return Arrays.stream(preferencesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(SurveyPreference::fromString)
                .collect(Collectors.toList());
    }

    private String serializePreferences(List<SurveyPreference> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return "";
        }
        
        return preferences.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }
}
