package de.tum.moodtrip_backend.controller;

import de.tum.moodtrip_backend.model.Recommendation;
import de.tum.moodtrip_backend.service.RecommendationService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/recommendations")
@Validated
public class RecommendationController {
    @Autowired
    private RecommendationService recommendationService;

    @PostMapping("/{conversationId}/generate")
    public Flux<Recommendation> generateRecommendations(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId) {
        return recommendationService.generateRecommendations(conversationId);
    }

    @GetMapping("/{conversationId}")
    public Flux<Recommendation> getRecommendations(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId) {
        return recommendationService.getRecommendationsByConversationId(conversationId);
    }

    @GetMapping("/{conversationId}/{type}")
    public Flux<Recommendation> getRecommendationsByType(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            @PathVariable String type) {
        return recommendationService.getRecommendationsByConversationIdAndType(conversationId, type);
    }
}

