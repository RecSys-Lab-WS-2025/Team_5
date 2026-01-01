package de.tum.moodtrip_backend.api.mapper;

import de.tum.moodtrip_backend.api.dto.SurveyRequest;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.SurveyDomain;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SurveyDtoMapper {
    

    public SurveyDomain requestToDomain(SurveyRequest request, Long conversationId, Long userId) {
        if (request == null) {
            return null;
        }
        
        List<PoiCategory> poiCategories = parsePoiCategories(request.poiCategories());
        
        return new SurveyDomain(
                null,
                userId,
                conversationId,
                request.latitude(),
                request.longitude(),
                request.locationName(),
                request.rangeMeters(),
                request.startDate(),
                request.endDate(),
                poiCategories,
                LocalDateTime.now()
        );
    }
    
    private List<PoiCategory> parsePoiCategories(List<String> poiCategories) {
        if (poiCategories == null || poiCategories.isEmpty()) {
            return Collections.emptyList();
        }
        
        return poiCategories.stream()
                .map(PoiCategory::fromDisplayName)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }
}
