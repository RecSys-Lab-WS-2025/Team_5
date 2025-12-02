package de.tum.moodtrip_backend.adapter_database.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;

import de.tum.moodtrip_backend.core.model.RouteRecommendationDomain;
import de.tum.moodtrip_backend.core.port.RouteRecommendationPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/test/route-recommendations")
@Tag(name = "Route Recommendation Testing", description = "Test endpoints for managing route recommendations (for development/testing purposes)")
public class RouteRecommendationTestController {

    private final RouteRecommendationPort routeRecommendationPort;


    public RouteRecommendationTestController(RouteRecommendationPort routeRecommendationPort) {
        this.routeRecommendationPort = routeRecommendationPort;

    }

    @Operation(summary = "Create a test route recommendation", description = "Creates a new route recommendation with GeoJSON data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Route recommendation created")
    })
    @PostMapping
    public Mono<RouteRecommendationDomain> create(@RequestBody CreateRouteRecommendationRequest request) {
        if (request.routeData() == null) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "routeData cannot be null"
            ));
        }

        RouteRecommendationDomain domain = new RouteRecommendationDomain(
                null,
                request.conversationId(),
                request.routeData(),
                null
        );
        return routeRecommendationPort.save(domain);
    }

    @Operation(summary = "Get route recommendation by ID", description = "Retrieves a route recommendation by its ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Route recommendation found")})
    @GetMapping("/{id}")
    public Mono<RouteRecommendationDomain> getById(
            @Parameter(description = "Route recommendation ID", required = true) @PathVariable Long id) {
        return routeRecommendationPort.findById(id);
    }

    @Operation(summary = "Get route recommendations by conversation", description = "Retrieves all route recommendations for a conversation")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Route recommendations retrieved")})
    @GetMapping("/conversation/{conversationId}")
    public Flux<RouteRecommendationDomain> getByConversationId(
            @Parameter(description = "Conversation ID", required = true) @PathVariable Long conversationId) {
        return routeRecommendationPort.findByConversationId(conversationId);
    }

    @Operation(summary = "Delete route recommendation", description = "Deletes a route recommendation by ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Route recommendation deleted")})
    @DeleteMapping("/{id}")
    public Mono<Void> delete(
            @Parameter(description = "Route recommendation ID", required = true) @PathVariable Long id) {
        return routeRecommendationPort.deleteById(id);
    }

    public record CreateRouteRecommendationRequest(
            Long conversationId,
            JsonNode routeData
    ) {
    }
}
