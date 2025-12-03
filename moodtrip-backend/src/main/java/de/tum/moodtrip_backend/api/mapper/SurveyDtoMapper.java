package de.tum.moodtrip_backend.api.mapper;

import de.tum.moodtrip_backend.api.dto.SurveyRequest;
import de.tum.moodtrip_backend.api.dto.SurveyResponse;
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
                request.location(),
                request.rangeMeters(),
                request.startDate(),
                request.endDate(),
                poiCategories,
                LocalDateTime.now()
        );
    }
    

    public SurveyResponse domainToResponse(SurveyDomain domain) {
        if (domain == null) {
            return null;
        }
        
        List<String> poiCategories = domain.poiCategories().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        
        return new SurveyResponse(
                domain.id(),
                domain.userId(),
                domain.conversationId(),
                domain.location(),
                domain.rangeMeters(),
                domain.startDate(),
                domain.endDate(),
                poiCategories,
                domain.createdAt()
        );
    }
    
    private List<PoiCategory> parsePoiCategories(List<String> poiCategories) {
        if (poiCategories == null || poiCategories.isEmpty()) {
            return Collections.emptyList();
        }
        
        return poiCategories.stream()
                .map(PoiCategory::fromDisplayName)
                .collect(Collectors.toList());
    }
}
