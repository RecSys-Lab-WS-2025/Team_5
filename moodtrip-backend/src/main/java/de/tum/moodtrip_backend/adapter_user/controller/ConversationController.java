package de.tum.moodtrip_backend.adapter_user.controller;

import de.tum.moodtrip_backend.core.model.EmotionResult;
import de.tum.moodtrip_backend.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.tum.moodtrip_backend.core.model.ConversationDomain;
import de.tum.moodtrip_backend.core.model.MessageDomain;
import de.tum.moodtrip_backend.core.model.Sender;
import de.tum.moodtrip_backend.core.service.ConversationDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/conversations")
@Validated
@Tag(name = "Conversation Management", description = "APIs for managing chat conversations and messages")
public class ConversationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationController.class);

    private final ConversationDomainService conversationService;
    private final JwtService jwtService;

    public ConversationController(ConversationDomainService conversationService, JwtService jwtService) {
        this.conversationService = conversationService;
        this.jwtService = jwtService;
    }


    @Operation(
        summary = "Start a new conversation",
        description = "Creates a new conversation for the authenticated user with a timestamped title",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Conversation started successfully",
            content = @Content(schema = @Schema(implementation = ConversationDomain.class))
        )
    })
    @PostMapping("/start")
    public Mono<ConversationDomain> startConversation(Authentication authentication) {
        Long userId = jwtService.extractUserId(authentication);
        return conversationService.startConversation(userId, "New Conversation-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    @Operation(
        summary = "Get current user's conversations",
        description = "Retrieves all conversations belonging to the authenticated user",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Conversations retrieved successfully",
            content = @Content(schema = @Schema(implementation = ConversationDomain.class))
        )
    })
    @GetMapping("/me")
    public Flux<ConversationDomain> getMyConversations(Authentication authentication) {
        Long userId = jwtService.extractUserId(authentication);
        return conversationService.getConversationsByUserId(userId);
    }

    @Operation(
        summary = "Get conversations by user ID",
        description = "Retrieves all conversations for a specific user. Users can only access their own conversations.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Conversations retrieved successfully",
            content = @Content(schema = @Schema(implementation = ConversationDomain.class))
        )
    })
    @GetMapping("/{userId}")
    public Flux<ConversationDomain> getConversations(
            @Parameter(description = "User ID", required = true) @PathVariable @NotNull(message = "User ID cannot be null") Long userId,
            Authentication authentication) {
        
        Long authenticatedUserId = jwtService.extractUserId(authentication);
        
        if (!userId.equals(authenticatedUserId)) {
            return Flux.error(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Access denied: You can only view your own conversations"
            ));
        }
        
        return conversationService.getConversationsByUserId(userId);
    }

    @Operation(
        summary = "Get messages in a conversation",
        description = "Retrieves all messages from a specific conversation. Users can only access messages from their own conversations.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Messages retrieved successfully",
            content = @Content(schema = @Schema(implementation = MessageDomain.class))
        )
    })
    @GetMapping("/{conversationId}/messages")
    public Flux<MessageDomain> getMessages(
            @Parameter(description = "Conversation ID", required = true) @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            Authentication authentication) {
        
        Long userId = jwtService.extractUserId(authentication);
        
        // Verify conversation belongs to user
        return conversationService.getConversationsByUserId(userId)
                .filter(conv -> conv.id().equals(conversationId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Conversation not found or access denied"
                )))
                .flatMap(conv -> conversationService.getMessagesByConversationId(conversationId));
    }


    @Operation(
        summary = "Extract emotion from user message",
        description = "Analyzes a user message to extract emotional content and sentiment using AI",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Emotion extracted successfully",
            content = @Content(schema = @Schema(implementation = EmotionResult.class))
        )
    })
    @PostMapping("/extract-emotion")
    public Mono<EmotionResult> extractEmotion(
            @Parameter(description = "Conversation ID", required = true) @RequestParam @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            @Parameter(description = "User message text", required = true) @RequestParam @NotBlank(message = "Message cannot be blank") String message,
            Authentication authentication) {
        
        Long userId = jwtService.extractUserId(authentication);
        
        LOGGER.info("Start extracting user emotions for userId: {}", userId);
        return conversationService.extractEmotion(conversationId, userId, message)
                .doOnSuccess(e -> LOGGER.info("Successfully extracted user emotions"))
                .doOnError(err -> LOGGER.error("Error while extracting emotions", err));
    }
}
