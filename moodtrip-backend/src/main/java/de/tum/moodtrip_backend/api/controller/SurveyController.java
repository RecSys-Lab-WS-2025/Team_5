package de.tum.moodtrip_backend.api.controller;


import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.tum.moodtrip_backend.api.dto.SurveyRequest;
import de.tum.moodtrip_backend.api.dto.SurveyResponse;
import de.tum.moodtrip_backend.api.mapper.SurveyDtoMapper;
import de.tum.moodtrip_backend.core.port.SurveyPort;
import de.tum.moodtrip_backend.core.service.ConversationDomainService;
import de.tum.moodtrip_backend.api.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/surveys")
@Validated
public class SurveyController {
    
    private final SurveyPort surveyPort;
    private final SurveyDtoMapper mapper;
    private final JwtService jwtService;
    private final ConversationDomainService conversationDomainService;
    
    public SurveyController(final SurveyPort surveyPort, final SurveyDtoMapper mapper, final JwtService jwtService, final ConversationDomainService conversationDomainService) {
        this.surveyPort = surveyPort;
        this.mapper = mapper;
        this.jwtService = jwtService;
        this.conversationDomainService = conversationDomainService;
    }
    

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SurveyResponse> submitSurvey(
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
                            .map(req -> mapper.requestToDomain(req, conversationId, userId))
                            .flatMap(surveyPort::save)
                            .map(mapper::domainToResponse)
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

    @GetMapping("/conversation/{conversationId}")
    public Flux<SurveyResponse> getConversationSurveys(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            Authentication authentication) {
        
        Long userId = jwtService.extractUserId(authentication);
        
        return surveyPort.findByConversationId(conversationId)
                .next()
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No survey found for this conversation"
                )))
                .flatMapMany(survey -> {
                    if (!survey.userId().equals(userId)) {
                        return Flux.error(new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Access denied: This conversation does not belong to you"
                        ));
                    }
                    return surveyPort.findByConversationId(conversationId)
                            .map(mapper::domainToResponse);
                }
                );
    }
    
    @GetMapping("/conversation/{conversationId}/latest")
    public Mono<SurveyResponse> getConversationLatestSurvey(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            Authentication authentication) {
        
        Long userId = jwtService.extractUserId(authentication);
        
        return surveyPort.findByConversationId(conversationId)
                .next()
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No survey found for this conversation"
                )))
                .flatMap(survey -> {
                    if (!survey.userId().equals(userId)) {
                        return Mono.error(new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Access denied: This conversation does not belong to you"
                        ));
                    }
                    return Mono.just(mapper.domainToResponse(survey));
                }
        );
                
        }
    
    @GetMapping("/user/me")
    public Flux<SurveyResponse> getMyUserSurveys(Authentication authentication) {
        Long userId = jwtService.extractUserId(authentication);
        return surveyPort.findByUserId(userId)
                .map(mapper::domainToResponse);
    }
    

    @GetMapping("/user/{userId}")
    public Flux<SurveyResponse> getUserSurveys(
            @PathVariable @NotNull(message = "User ID cannot be null") Long userId,
            Authentication authentication) {
        
        Long authenticatedUserId = jwtService.extractUserId(authentication);
        
        return surveyPort.findByUserId(userId)
                .collectList()
                .flatMapMany(surveys -> {
                    if (surveys.isEmpty()) {
                        return Flux.error(new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No survey found for this user"
                        ));
                    }
                    if (!surveys.get(0).userId().equals(authenticatedUserId)) {
                        return Flux.error(new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Access denied: You can only view your own surveys"
                        ));
                    }
                    return Flux.fromIterable(surveys)
                            .map(mapper::domainToResponse);
                });
    }
    
    @GetMapping("/user/me/latest")
    public Mono<SurveyResponse> getMyLatestSurvey(Authentication authentication) {
        Long userId = jwtService.extractUserId(authentication);
        return surveyPort.findLatestByUserId(userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No survey found for this user"
                )))
                .map(mapper::domainToResponse);
    }
    
    @GetMapping("/user/{userId}/latest")
    public Mono<SurveyResponse> getUserLatestSurvey(
            @PathVariable @NotNull(message = "User ID cannot be null") Long userId,
            Authentication authentication) {
        
        Long authenticatedUserId = jwtService.extractUserId(authentication);
        
        return surveyPort.findLatestByUserId(userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No survey found for this user"
                )))
                .flatMap(survey -> {
                    if (!survey.userId().equals(authenticatedUserId)) {
                        return Mono.error(new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Access denied: You can only view your own surveys"
                        ));
                    }
                    return Mono.just(mapper.domainToResponse(survey));
                });
    }
    

    @GetMapping("/{id}")
    public Mono<SurveyResponse> getSurveyById(
            @PathVariable @NotNull(message = "Survey ID cannot be null") Long id,
            Authentication authentication) {
        
        Long userId = jwtService.extractUserId(authentication);
        
        return surveyPort.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Survey not found"
                )))
                .flatMap(survey -> {
                    if (!survey.userId().equals(userId)) {
                        return Mono.error(new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Access denied: This survey does not belong to you"
                        ));
                    }
                    return Mono.just(mapper.domainToResponse(survey));
                });
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteSurvey(
            @PathVariable @NotNull(message = "Survey ID cannot be null") Long id,
            Authentication authentication) {
        
        Long userId = jwtService.extractUserId(authentication);
        
        return surveyPort.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Survey not found"
                )))
                .flatMap(survey -> {
                    if (!survey.userId().equals(userId)) {
                        return Mono.error(new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Access denied: You can only delete your own surveys"
                        ));
                    }
                    return surveyPort.deleteById(id);
                });
    }


}
