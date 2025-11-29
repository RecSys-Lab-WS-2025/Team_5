package de.tum.moodtrip_backend.adapter_user.controller;

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

import de.tum.moodtrip_backend.adapter_user.dto.SurveyRequest;
import de.tum.moodtrip_backend.adapter_user.dto.SurveyResponse;
import de.tum.moodtrip_backend.adapter_user.mapper.SurveyDtoMapper;
import de.tum.moodtrip_backend.core.port.SurveyPort;
import de.tum.moodtrip_backend.security.JwtToken;
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
    
    public SurveyController(final SurveyPort surveyPort, final SurveyDtoMapper mapper) {
        this.surveyPort = surveyPort;
        this.mapper = mapper;
    }
    

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SurveyResponse> submitSurvey(
            @Valid @RequestBody SurveyRequest request,
            @RequestParam @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        
        return Mono.just(request)
                .map(req -> mapper.requestToDomain(req, conversationId, userId))
                .flatMap(surveyPort::save)
                .map(mapper::domainToResponse)
                .onErrorResume(org.springframework.dao.DataIntegrityViolationException.class, ex -> {
                    // Handle unique constraint violation for conversation_id
                    if (ex.getMessage() != null && ex.getMessage().contains("conversation_id")) {
                        return Mono.error(new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Survey already exists for this conversation"
                        ));
                    }
                    return Mono.error(ex);
                });
    }

    @GetMapping("/conversation/{conversationId}")
    public Flux<SurveyResponse> getConversationSurveys(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        
        return surveyPort.findByConversationId(conversationId)
                .filter(survey -> survey.userId().equals(userId))
                .switchIfEmpty(Flux.error(new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied: This conversation does not belong to you"
                )))
                .map(mapper::domainToResponse);
    }
    
    @GetMapping("/conversation/{conversationId}/latest")
    public Mono<SurveyResponse> getConversationLatestSurvey(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        
        return surveyPort.findByConversationId(conversationId)
                .next()
                .filter(survey -> survey.userId().equals(userId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied: This conversation does not belong to you"
                )))
                .map(mapper::domainToResponse);
    }
    
    @GetMapping("/user/me")
    public Flux<SurveyResponse> getMyUserSurveys(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return surveyPort.findByUserId(userId)
                .map(mapper::domainToResponse);
    }
    

    @GetMapping("/user/{userId}")
    public Flux<SurveyResponse> getUserSurveys(
            @PathVariable @NotNull(message = "User ID cannot be null") Long userId,
            Authentication authentication) {
        
        Long authenticatedUserId = extractUserId(authentication);
        
        // Only allow users to access their own surveys
        if (!userId.equals(authenticatedUserId)) {
            return Flux.error(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Access denied: You can only view your own surveys"
            ));
        }
        
        return surveyPort.findByUserId(userId)
                .map(mapper::domainToResponse);
    }
    
    @GetMapping("/user/me/latest")
    public Mono<SurveyResponse> getMyLatestSurvey(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return surveyPort.findLatestByUserId(userId)
                .map(mapper::domainToResponse);
    }
    
    @GetMapping("/user/{userId}/latest")
    public Mono<SurveyResponse> getUserLatestSurvey(
            @PathVariable @NotNull(message = "User ID cannot be null") Long userId,
            Authentication authentication) {
        
        Long authenticatedUserId = extractUserId(authentication);
        
        if (!userId.equals(authenticatedUserId)) {
            return Mono.error(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Access denied: You can only view your own surveys"
            ));
        }
        
        return surveyPort.findLatestByUserId(userId)
                .map(mapper::domainToResponse);
    }
    

    @GetMapping("/{id}")
    public Mono<SurveyResponse> getSurveyById(
            @PathVariable @NotNull(message = "Survey ID cannot be null") Long id,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        
        return surveyPort.findById(id)
                .filter(survey -> survey.userId().equals(userId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied: This survey does not belong to you"
                )))
                .map(mapper::domainToResponse);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteSurvey(
            @PathVariable @NotNull(message = "Survey ID cannot be null") Long id,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        
        // Verify ownership before deletion
        return surveyPort.findById(id)
                .filter(survey -> survey.userId().equals(userId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied: You can only delete your own surveys"
                )))
                .flatMap(survey -> surveyPort.deleteById(id));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authentication required"
            );
        }
        
        if (!(authentication instanceof JwtToken jwtToken)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid authentication type"
            );
        }
        
        return jwtToken.getUserId();
    }
}
