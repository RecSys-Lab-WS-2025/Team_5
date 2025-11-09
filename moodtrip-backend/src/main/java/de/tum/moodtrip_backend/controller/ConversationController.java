package de.tum.moodtrip_backend.controller;

import de.tum.moodtrip_backend.model.Conversation;
import de.tum.moodtrip_backend.model.Message;
import de.tum.moodtrip_backend.service.ConversationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/conversations")
@Validated
public class ConversationController {
    @Autowired
    private ConversationService service;


    @PostMapping("/start")
    public Mono<Conversation> startConversation(
            @RequestParam @NotBlank(message = "User ID cannot be blank") String userId, 
            @RequestParam @NotBlank(message = "Title cannot be blank") String title) {
        return service.startConversation(userId, title);
    }

    @GetMapping("/{userId}")
    public Flux<Conversation> getConversations(
            @PathVariable @NotBlank(message = "User ID cannot be blank") String userId) {
        return service.getConversationsByUserId(userId);
    }

    @GetMapping("/{conversationId}/messages")
    public Flux<Message> getMessages(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId) {
        return service.getMessagesByConversationId(conversationId);
    }

    @PostMapping("/{conversationId}/messages")
    public Mono<Message> addMessage(
            @PathVariable @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            @RequestParam @NotBlank(message = "Sender cannot be blank") String sender,
            @RequestBody @NotBlank(message = "Content cannot be blank") String content) {
        return service.addMessage(conversationId, sender, content);
    }


}
