package de.tum.moodtrip_backend.api.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import de.tum.moodtrip_backend.api.dto.MessageContentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import de.tum.moodtrip_backend.api.security.JwtService;
import de.tum.moodtrip_backend.core.model.ConversationDomain;
import de.tum.moodtrip_backend.core.model.EmotionResult;
import de.tum.moodtrip_backend.core.model.MessageDomain;
import de.tum.moodtrip_backend.core.service.ConversationDomainService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/conversations")
@Validated
public class ConversationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationController.class);

    private final ConversationDomainService conversationService;
    private final JwtService jwtService;

    public ConversationController(ConversationDomainService conversationService, JwtService jwtService) {
        this.conversationService = conversationService;
        this.jwtService = jwtService;
    }

    /**
     * Start a new conversation for the authenticated user.
     */
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ConversationDomain> startConversation(Authentication authentication) {
        Long userId = jwtService.extractUserId(authentication);
        String initialTitle = "New Conversation-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return conversationService.startConversation(userId, initialTitle);
    }

    /**
     * Get all conversations for the authenticated user.
     */
    @GetMapping("/me")
    public Flux<ConversationDomain> getMyConversations(Authentication authentication) {
        Long userId = jwtService.extractUserId(authentication);
        return conversationService.getConversationsByUserId(userId);
    }

    /**
     * Get all conversations for the given user (must be the authenticated user).
     * This endpoint is basically an explicit version of /me.
     */
    @GetMapping("/{userId}")
    public Flux<ConversationDomain> getConversations(
            @PathVariable @NotNull(message = "User ID cannot be null") Long userId,
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

    /**
     * Get all messages of a specific conversation for the authenticated user.
     * Ownership is validated by checking that the conversation belongs to the user.
     */
    @GetMapping("/{conversationId}/messages")
    public Flux<MessageDomain> getMessages(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            Authentication authentication) {

        Long userId = jwtService.extractUserId(authentication);

        // Validate that this conversation belongs to the authenticated user
        return conversationService.getConversationsByUserId(userId)
                .filter(conv -> conv.id().equals(conversationId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conversation not found or access denied"
                )))
                .flatMap(conv -> conversationService.getMessagesByConversationId(conversationId));
    }

    /**
     * Extract the user's emotion based on the latest message and conversation history.
     * Ownership checks are handled inside the service method.
     */
    @PostMapping("/extract-emotion")
    public Mono<EmotionResult> extractEmotion(
            @RequestParam @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            @RequestParam @NotBlank(message = "Message cannot be blank") String message,
            Authentication authentication) {

        Long userId = jwtService.extractUserId(authentication);

        LOGGER.info("Start extracting user emotions for userId: {}", userId);
        return conversationService.extractEmotion(conversationId, userId, message)
                .doOnSuccess(e -> LOGGER.info("Successfully extracted user emotions {}", e))
                .doOnError(err -> LOGGER.error("Error while extracting emotions", err));
    }

    /**
     * Generate a title for the given conversation using the conversation transcript.
     * Ownership checks are handled inside the service method.
     */
    @PostMapping("/{conversationId}/title")
    public Mono<String> generateConversationTitle(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            Authentication authentication) {

        Long userId = jwtService.extractUserId(authentication);
        return conversationService.generateConversationTitle(conversationId, userId);
    }

    /**
     * Update the title of a conversation for the authenticated user.
     * Ownership checks are handled inside the service method.
     */
    @PutMapping("/{conversationId}/title")
    public Mono<ConversationDomain> updateConversationTitle(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            @RequestParam("title") @NotBlank(message = "Title cannot be blank") String title,
            Authentication authentication) {

        Long userId = jwtService.extractUserId(authentication);
        return conversationService.updateConversationTitle(conversationId, userId, title);
    }

    /**
     * Add a message (user or bot) to the conversation.
     * This endpoint checks that the conversation belongs to the authenticated user
     * before delegating to the domain service.
     */
    @PostMapping("/{conversationId}/message")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MessageDomain> addMessage(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            @RequestBody MessageContentRequest request,
            Authentication authentication) {

        Long userId = jwtService.extractUserId(authentication);
        String content = request.content();
        boolean isUser = request.isUser();

        // Verify ownership of the conversation here before adding the message
        return conversationService.getConversationsByUserId(userId)
                .filter(conv -> conv.id().equals(conversationId))
                .next()
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conversation not found or access denied"
                )))
                .flatMap(conv -> conversationService.addMessage(conversationId, content, isUser));
    }


    /**
     * Delete a conversation for the authenticated user.
     * Ownership checks and actual deletion logic are handled by the service.
     */
    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteConversation(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            Authentication authentication) {

        Long userId = jwtService.extractUserId(authentication);
        return conversationService.deleteConversation(conversationId, userId);
    }
}
