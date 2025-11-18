package de.tum.moodtrip_backend.adapter_user.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.tum.moodtrip_backend.core.model.RecommendationDomain;
import de.tum.moodtrip_backend.core.service.RecommendationDomainService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/recommendations")
@Validated
public class RecommendationController {
    private final RecommendationDomainService recommendationService;

    public RecommendationController(RecommendationDomainService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public Mono<RecommendationDomain> createRecommendation(@Valid @RequestBody CreateRecommendationRequest request) {
        return recommendationService.createRecommendation(
            request.conversationId(),
            request.type(),
            request.title(),
            request.description(),
            request.link(),
            request.trackId(),
            request.routeData()
        );
    }

    @GetMapping("/{conversationId}")
    public Flux<RecommendationDomain> getRecommendations(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId) {
        return recommendationService.getRecommendationsByConversationId(conversationId);
    }

    @GetMapping("/{conversationId}/{type}")
    public Flux<RecommendationDomain> getRecommendationsByType(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            @PathVariable String type) {
        return recommendationService.getRecommendationsByConversationIdAndType(conversationId, type);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteRecommendation(@PathVariable Long id) {
        return recommendationService.deleteRecommendation(id);
    }
    
    // DTO for request body
    public record CreateRecommendationRequest(
        @NotNull Long conversationId,
        @NotNull String type,
        @NotNull String title,
        String description,
        String link,
        String trackId,
        String routeData
    ) {}
}
