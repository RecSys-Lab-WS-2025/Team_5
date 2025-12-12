package de.tum.moodtrip_backend.core.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.moodtrip_backend.core.model.PoiRouteResult;
import de.tum.moodtrip_backend.core.model.RouteRecommendationDomain;

public final class PoiRouteResultRouteRecommendationMapper {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private PoiRouteResultRouteRecommendationMapper() {
        // utility class
    }

    public static RouteRecommendationDomain toDomain(PoiRouteResult poiRouteResult, long conversationId) {
        return new RouteRecommendationDomain(
                null,
                conversationId,
                objectMapper.valueToTree(poiRouteResult),
                null
        );
    }
}
