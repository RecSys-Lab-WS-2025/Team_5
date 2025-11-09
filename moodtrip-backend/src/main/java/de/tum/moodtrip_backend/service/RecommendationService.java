package de.tum.moodtrip_backend.service;


import de.tum.moodtrip_backend.model.Recommendation;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

@Service
public class RecommendationService {
    public Flux<Recommendation> generateRecommendations(Long conversationId) {
        // TODO: Implement recommendation generation logic
        return Flux.empty();
    }
    public Flux<Recommendation> getRecommendationsByConversationId(Long conversationId) {
        // TODO: Implement retrieval logic
        return Flux.empty();
    }
    public Flux<Recommendation> getRecommendationsByConversationIdAndType(Long conversationId, String type) {
        // TODO: Implement retrieval logic by type
        return Flux.empty();
    }
    
}
