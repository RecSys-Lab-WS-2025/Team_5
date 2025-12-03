package de.tum.moodtrip_backend.api.controller;

import de.tum.moodtrip_backend.core.model.EmotionResult;
import de.tum.moodtrip_backend.api.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.tum.moodtrip_backend.core.model.ConversationDomain;
import de.tum.moodtrip_backend.core.model.MessageDomain;
import de.tum.moodtrip_backend.core.service.ConversationDomainService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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


    @PostMapping("/start")
    public Mono<ConversationDomain> startConversation(Authentication authentication) {
        Long userId = jwtService.extractUserId(authentication);
        return conversationService.startConversation(userId, "New Conversation-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    @GetMapping("/me")
    public Flux<ConversationDomain> getMyConversations(Authentication authentication) {
        Long userId = jwtService.extractUserId(authentication);
        return conversationService.getConversationsByUserId(userId);
    }

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

    @GetMapping("/{conversationId}/messages")
    public Flux<MessageDomain> getMessages(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
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


    @PostMapping("/extract-emotion")
    public Mono<EmotionResult> extractEmotion(
            @RequestParam @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            @RequestParam @NotBlank(message = "Message cannot be blank") String message,
            Authentication authentication) {
        
        Long userId = jwtService.extractUserId(authentication);
        
        LOGGER.info("Start extracting user emotions for userId: {}", userId);
        return conversationService.extractEmotion(conversationId, userId, message)
                .doOnSuccess(e -> LOGGER.info("Successfully extracted user emotions"))
                .doOnError(err -> LOGGER.error("Error while extracting emotions", err));
    }
}
