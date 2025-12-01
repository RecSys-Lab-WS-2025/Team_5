package de.tum.moodtrip_backend.adapter_user.mapper;

import de.tum.moodtrip_backend.adapter_user.dto.SurveyRequest;
import de.tum.moodtrip_backend.adapter_user.dto.SurveyResponse;
import de.tum.moodtrip_backend.core.model.SurveyDomain;
import de.tum.moodtrip_backend.core.model.SurveyPreference;
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
        
        List<SurveyPreference> preferences = parsePreferences(request.preferences());
        
        return new SurveyDomain(
                null,
                userId,
                conversationId,
                request.location(),
                request.rangeMeters(),
                request.startDate(),
                request.endDate(),
                preferences,
                LocalDateTime.now()
        );
    }
    

    public SurveyResponse domainToResponse(SurveyDomain domain) {
        if (domain == null) {
            return null;
        }
        
        List<String> preferences = domain.preferences().stream()
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
                preferences,
                domain.createdAt()
        );
    }
    
    private List<SurveyPreference> parsePreferences(List<String> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return Collections.emptyList();
        }
        
        return preferences.stream()
                .map(SurveyPreference::fromString)
                .collect(Collectors.toList());
    }
}
