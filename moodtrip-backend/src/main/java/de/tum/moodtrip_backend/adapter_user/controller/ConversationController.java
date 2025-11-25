package de.tum.moodtrip_backend.adapter_user.controller;

import de.tum.moodtrip_backend.core.model.EmotionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.tum.moodtrip_backend.core.model.ConversationDomain;
import de.tum.moodtrip_backend.core.model.MessageDomain;
import de.tum.moodtrip_backend.core.model.Sender;
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

    public ConversationController(ConversationDomainService conversationService) {
        this.conversationService = conversationService;
    }


    @PostMapping("/start")
    public Mono<ConversationDomain> startConversation(
            @RequestParam @NotNull(message = "User ID cannot be null") Long userId) {
        return conversationService.startConversation(userId, "New Conversation-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    @GetMapping("/{userId}")
    public Flux<ConversationDomain> getConversations(
            @PathVariable @NotNull(message = "User ID cannot be null") Long userId) {
        return conversationService.getConversationsByUserId(userId);
    }

    @GetMapping("/{conversationId}/messages")
    public Flux<MessageDomain> getMessages(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId) {
        return conversationService.getMessagesByConversationId(conversationId);
    }



    @PostMapping("/extract-emotion")
    public Mono<EmotionResult> extractEmotion(
            @RequestParam @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            @RequestParam @NotNull(message = "User ID cannot be null") Long userId,
            @RequestParam @NotBlank(message = "Message cannot be blank") String message) {
        LOGGER.info("Start extracting user emotions");
        return conversationService.extractEmotion(conversationId, userId, message)
                .doOnSuccess(e -> LOGGER.info("Successfully extracted user emotions"))
                .doOnError(err -> LOGGER.error("Error while extracting emotions", err));
    }
}
