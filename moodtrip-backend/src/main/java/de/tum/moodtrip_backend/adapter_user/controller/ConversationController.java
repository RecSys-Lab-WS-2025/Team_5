package de.tum.moodtrip_backend.adapter_user.controller;

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
import de.tum.moodtrip_backend.core.service.ConversationDomainService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/conversations")
@Validated
public class ConversationController {
    private final ConversationDomainService conversationService;

    public ConversationController(ConversationDomainService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/start")
    public Mono<ConversationDomain> startConversation(
            @RequestParam @NotBlank(message = "User ID cannot be blank") String userId,
            @RequestParam @NotBlank(message = "Title cannot be blank") String title) {
        return conversationService.startConversation(userId, title);
    }

    @GetMapping("/{userId}")
    public Flux<ConversationDomain> getConversations(
            @PathVariable @NotBlank(message = "User ID cannot be blank") String userId) {
        return conversationService.getConversationsByUserId(userId);
    }

    @GetMapping("/{conversationId}/messages")
    public Flux<MessageDomain> getMessages(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId) {
        return conversationService.getMessagesByConversationId(conversationId);
    }

    @PostMapping("/{conversationId}/messages")
    public Mono<MessageDomain> addMessage(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            @RequestParam @NotBlank(message = "Sender cannot be blank") String sender,
            @RequestBody @NotBlank(message = "Content cannot be blank") String content) {
        return conversationService.addMessage(conversationId, sender, content);
    }
}
