package de.tum.moodtrip_backend.core.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tum.moodtrip_backend.core.model.RecommendationDomain;
import de.tum.moodtrip_backend.core.port.RecommendationPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RecommendationDomainService {
    private final RecommendationPort recommendationPort;
    private final ObjectMapper objectMapper;

    public RecommendationDomainService(RecommendationPort recommendationPort, ObjectMapper objectMapper) {
        this.recommendationPort = recommendationPort;
        this.objectMapper = objectMapper;
    }

    public Mono<RecommendationDomain> saveRecommendation(RecommendationDomain recommendation) {
        if (recommendation.createdAt() == null) {
            recommendation = new RecommendationDomain(
                    recommendation.id(),
                    recommendation.conversationId(),
                    recommendation.type(),
                    recommendation.title(),
                    recommendation.description(),
                    recommendation.link(),
                    recommendation.trackId(),
                    recommendation.routeData(),
                    LocalDateTime.now()
            );
        }
        return recommendationPort.save(recommendation);
    }

    public Mono<RecommendationDomain> createRecommendation(Long conversationId, String type, 
                                                           String title, String description, String link, 
                                                           String trackId, String routeData) {
        // Validate routeData is valid JSON if provided
        if (routeData != null && !routeData.isBlank()) {
            try {
                objectMapper.readTree(routeData);
            } catch (Exception e) {
                return Mono.error(new IllegalArgumentException(
                    "Invalid routeData: must be valid JSON. Error: " + e.getMessage()));
            }
        }
        
        RecommendationDomain recommendation = new RecommendationDomain(
                null,
                conversationId,
                type,
                title,
                description,
                link,
                trackId,
                routeData,
                LocalDateTime.now()
        );
        return recommendationPort.save(recommendation);
    }

    public Flux<RecommendationDomain> getRecommendationsByConversationId(Long conversationId) {
        return recommendationPort.findByConversationId(conversationId);
    }
    
    public Flux<RecommendationDomain> getRecommendationsByConversationIdAndType(Long conversationId, String type) {
        return recommendationPort.findByConversationIdAndType(conversationId, type);
    }

    public Flux<RecommendationDomain> getRecommendationsByType(String type) {
        return recommendationPort.findByType(type);
    }

    public Mono<Long> countByConversationId(Long conversationId) {
        return recommendationPort.countByConversationId(conversationId);
    }

    public Mono<Void> deleteRecommendation(Long id) {
        return recommendationPort.deleteById(id);
    }
}
