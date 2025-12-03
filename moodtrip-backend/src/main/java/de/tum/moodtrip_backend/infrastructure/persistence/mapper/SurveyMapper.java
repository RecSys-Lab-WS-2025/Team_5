package de.tum.moodtrip_backend.infrastructure.persistence.mapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import de.tum.moodtrip_backend.core.model.PoiCategory;
import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.SurveyEntity;
import de.tum.moodtrip_backend.core.model.SurveyDomain;


@Component
public class SurveyMapper {

    public SurveyDomain toDomain(final SurveyEntity entity) {
        if (entity == null) {
            return null;
        }
        
        List<PoiCategory> poiCategories = parsPoiCategories(entity.getPreferences());
        
        return new SurveyDomain(
                entity.getId(),
                entity.getUserId(),
                entity.getConversationId(),
                entity.getLocation(),
                entity.getRangeMeters(),
                entity.getStartDate(),
                entity.getEndDate(),
                poiCategories,
                entity.getCreatedAt()
        );
    }

    public SurveyEntity toEntity(final SurveyDomain domain) {
        if (domain == null) {
            return null;
        }
        
        String poiCategoriesStr = serializePoiCategories(domain.poiCategories());
        LocalDateTime createdAt = domain.createdAt() != null ? domain.createdAt() : LocalDateTime.now();
        
        return new SurveyEntity(
                domain.id(),
                domain.userId(),
                domain.conversationId(),
                domain.location(),
                domain.rangeMeters(),
                domain.startDate(),
                domain.endDate(),
                poiCategoriesStr,
                createdAt
        );
    }

    private List<PoiCategory> parsPoiCategories(String poiCategoriesStr) {
        if (poiCategoriesStr == null || poiCategoriesStr.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return Arrays.stream(poiCategoriesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(PoiCategory::fromDisplayName)
                .collect(Collectors.toList());
    }

    private String serializePoiCategories(List<PoiCategory> poiCategories) {
        if (poiCategories == null || poiCategories.isEmpty()) {
            return "";
        }
        
        return poiCategories.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }
}
