package de.tum.moodtrip_backend.api.controller;


import de.tum.moodtrip_backend.api.mapper.GeoJsonRouteMapper;
import de.tum.moodtrip_backend.core.service.RouteService;
import org.geojson.FeatureCollection;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import reactor.core.publisher.Mono;

import de.tum.moodtrip_backend.api.dto.SurveyRequest;
import de.tum.moodtrip_backend.api.mapper.SurveyDtoMapper;
import de.tum.moodtrip_backend.api.security.JwtService;
import de.tum.moodtrip_backend.core.port.SurveyPort;
import de.tum.moodtrip_backend.core.service.ConversationDomainService;


@RestController
@RequestMapping("/api/surveys")
@Validated
public class SurveyController {

    private final SurveyPort surveyPort;
    private final SurveyDtoMapper surveyDtoMapper;
    private final GeoJsonRouteMapper geoJsonRouteMapper;
    private final JwtService jwtService;
    private final ConversationDomainService conversationDomainService;
    private final RouteService routeService;

    public SurveyController(final SurveyPort surveyPort, final SurveyDtoMapper surveyDtoMapper, GeoJsonRouteMapper geoJsonRouteMapper, final JwtService jwtService, final ConversationDomainService conversationDomainService, RouteService routeService) {
        this.surveyPort = surveyPort;
        this.surveyDtoMapper = surveyDtoMapper;
        this.geoJsonRouteMapper = geoJsonRouteMapper;
        this.jwtService = jwtService;
        this.conversationDomainService = conversationDomainService;
        this.routeService = routeService;
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<FeatureCollection> submitSurvey(
            @Valid @RequestBody SurveyRequest request,
            @RequestParam @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            Authentication authentication) {

        Long userId = jwtService.extractUserId(authentication);

        return conversationDomainService.getConversationById(conversationId)
                .flatMap(conversation -> {
                    if (!conversation.userId().equals(userId)) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Access denied: This conversation does not belong to you"
                        ));
                    }
                    return Mono.just(request)
                            .map(req -> surveyDtoMapper.requestToDomain(req, conversationId, userId))
                            .flatMap(surveyPort::save)
                            .flatMap(surveyDomain -> routeService.getRoute(conversationId,
                                    surveyDomain.latitude(), surveyDomain.longitude(), surveyDomain.poiCategories(),
                                    surveyDomain.rangeMeters()))
                            .map(geoJsonRouteMapper::toFeatureCollection)
                            .onErrorResume(org.springframework.dao.DataIntegrityViolationException.class, ex -> {
                                if (ex.getMessage() != null && ex.getMessage().contains("conversation_id")) {
                                    return Mono.error(new ResponseStatusException(
                                            HttpStatus.CONFLICT,
                                            "Survey already exists for this conversation"
                                    ));
                                }
                                return Mono.error(ex);
                            });
                });
    }
}
