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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/test/route-recommendations")
public class RouteRecommendationTestController {

    private final RouteRecommendationPort routeRecommendationPort;


    public RouteRecommendationTestController(RouteRecommendationPort routeRecommendationPort) {
        this.routeRecommendationPort = routeRecommendationPort;

    }

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

    @GetMapping("/{id}")
    public Mono<RouteRecommendationDomain> getById(@PathVariable Long id) {
        return routeRecommendationPort.findById(id);
    }

    @GetMapping("/conversation/{conversationId}")
    public Flux<RouteRecommendationDomain> getByConversationId(@PathVariable Long conversationId) {
        return routeRecommendationPort.findByConversationId(conversationId);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable Long id) {
        return routeRecommendationPort.deleteById(id);
    }

    public record CreateRouteRecommendationRequest(
            Long conversationId,
            JsonNode routeData
    ) {
    }
}
