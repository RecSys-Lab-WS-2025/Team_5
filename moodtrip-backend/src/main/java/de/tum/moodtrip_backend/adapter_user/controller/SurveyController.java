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
import de.tum.moodtrip_backend.core.service.ConversationDomainService;
import de.tum.moodtrip_backend.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/surveys")
@Validated
@Tag(name = "Survey Management", description = "APIs for managing user surveys and emotional assessments")
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
    

    @Operation(
        summary = "Submit a new survey",
        description = "Creates a new survey for a specific conversation with emotional and personal assessments",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Survey created successfully",
            content = @Content(schema = @Schema(implementation = SurveyResponse.class))
        )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SurveyResponse> submitSurvey(
            @Valid @RequestBody SurveyRequest request,
            @Parameter(description = "Conversation ID", required = true) @RequestParam @NotNull(message = "Conversation ID cannot be null") Long conversationId,
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

    @Operation(
        summary = "Get surveys for a conversation",
        description = "Retrieves all surveys associated with a specific conversation. Users can only access surveys for their own conversations.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Surveys retrieved successfully",
            content = @Content(schema = @Schema(implementation = SurveyResponse.class))
        )
    })
    @GetMapping("/conversation/{conversationId}")
    public Flux<SurveyResponse> getConversationSurveys(
            @Parameter(description = "Conversation ID", required = true) @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
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
    
    @Operation(
        summary = "Get latest survey for a conversation",
        description = "Retrieves the most recent survey for a specific conversation. Users can only access surveys for their own conversations.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Latest survey retrieved successfully",
            content = @Content(schema = @Schema(implementation = SurveyResponse.class))
        )
    })
    @GetMapping("/conversation/{conversationId}/latest")
    public Mono<SurveyResponse> getConversationLatestSurvey(
            @Parameter(description = "Conversation ID", required = true) @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
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
    
    @Operation(
        summary = "Get current user's surveys",
        description = "Retrieves all surveys belonging to the authenticated user",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Surveys retrieved successfully",
            content = @Content(schema = @Schema(implementation = SurveyResponse.class))
        )
    })
    @GetMapping("/user/me")
    public Flux<SurveyResponse> getMyUserSurveys(Authentication authentication) {
        Long userId = jwtService.extractUserId(authentication);
        return surveyPort.findByUserId(userId)
                .map(mapper::domainToResponse);
    }
    

    @Operation(
        summary = "Get surveys for a specific user",
        description = "Retrieves all surveys for a specific user. Users can only access their own surveys.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Surveys retrieved successfully",
            content = @Content(schema = @Schema(implementation = SurveyResponse.class))
        )
    })
    @GetMapping("/user/{userId}")
    public Flux<SurveyResponse> getUserSurveys(
            @Parameter(description = "User ID", required = true) @PathVariable @NotNull(message = "User ID cannot be null") Long userId,
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
    
    @Operation(
        summary = "Get current user's latest survey",
        description = "Retrieves the most recent survey submitted by the authenticated user",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Latest survey retrieved successfully",
            content = @Content(schema = @Schema(implementation = SurveyResponse.class))
        )
    })
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
    
    @Operation(
        summary = "Get a specific user's latest survey",
        description = "Retrieves the most recent survey for a specific user. Users can only access their own surveys.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Latest survey retrieved successfully",
            content = @Content(schema = @Schema(implementation = SurveyResponse.class))
        )
    })
    @GetMapping("/user/{userId}/latest")
    public Mono<SurveyResponse> getUserLatestSurvey(
            @Parameter(description = "User ID", required = true) @PathVariable @NotNull(message = "User ID cannot be null") Long userId,
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
    

    @Operation(
        summary = "Get survey by ID",
        description = "Retrieves a specific survey by its ID. Users can only access their own surveys.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Survey retrieved successfully",
            content = @Content(schema = @Schema(implementation = SurveyResponse.class))
        )
    })
    @GetMapping("/{id}")
    public Mono<SurveyResponse> getSurveyById(
            @Parameter(description = "Survey ID", required = true) @PathVariable @NotNull(message = "Survey ID cannot be null") Long id,
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

    @Operation(
        summary = "Delete a survey",
        description = "Deletes a survey by its ID. Users can only delete their own surveys.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Survey deleted successfully"
        )
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteSurvey(
            @Parameter(description = "Survey ID", required = true) @PathVariable @NotNull(message = "Survey ID cannot be null") Long id,
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
