package de.tum.moodtrip_backend.adapter_database.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.tum.moodtrip_backend.core.model.MusicRecommendationDomain;
import de.tum.moodtrip_backend.core.port.MusicRecommendationPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/test/music-recommendations")
@Tag(name = "Music Recommendation Testing", description = "Test endpoints for managing music recommendations (for development/testing purposes)")
public class MusicRecommendationTestController {

    private final MusicRecommendationPort musicRecommendationPort;

    public MusicRecommendationTestController(MusicRecommendationPort musicRecommendationPort) {
        this.musicRecommendationPort = musicRecommendationPort;
    }

    @Operation(summary = "Create a test music recommendation", description = "Creates a new music recommendation entry for testing")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Music recommendation created")})
    @PostMapping
    public Mono<MusicRecommendationDomain> create(@RequestBody CreateMusicRecommendationRequest request) {
        MusicRecommendationDomain domain = new MusicRecommendationDomain(
                null,
                request.conversationId(),
                request.title(),
                request.link(),
                null
        );
        return musicRecommendationPort.save(domain);
    }

    @Operation(summary = "Get music recommendation by ID", description = "Retrieves a music recommendation by its ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Music recommendation found")})
    @GetMapping("/{id}")
    public Mono<MusicRecommendationDomain> getById(
            @Parameter(description = "Music recommendation ID", required = true) @PathVariable Long id) {
        return musicRecommendationPort.findById(id);
    }

    @Operation(summary = "Get music recommendations by conversation", description = "Retrieves all music recommendations for a conversation")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Music recommendations retrieved")})
    @GetMapping("/conversation/{conversationId}")
    public Flux<MusicRecommendationDomain> getByConversationId(
            @Parameter(description = "Conversation ID", required = true) @PathVariable Long conversationId) {
        return musicRecommendationPort.findByConversationId(conversationId);
    }

    @Operation(summary = "Delete music recommendation", description = "Deletes a music recommendation by ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Music recommendation deleted")})
    @DeleteMapping("/{id}")
    public Mono<Void> delete(
            @Parameter(description = "Music recommendation ID", required = true) @PathVariable Long id) {
        return musicRecommendationPort.deleteById(id);
    }

    public record CreateMusicRecommendationRequest(
            Long conversationId,
            String title,
            String link
    ) {
    }
}
