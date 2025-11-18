package de.tum.moodtrip_backend.core.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import de.tum.moodtrip_backend.adapter_chatbot.service.EmotionService;
import de.tum.moodtrip_backend.core.model.ConversationDomain;
import de.tum.moodtrip_backend.core.model.MessageDomain;
import de.tum.moodtrip_backend.core.port.ConversationPort;
import de.tum.moodtrip_backend.exception.ResourceNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ConversationDomainService {
    private final ConversationPort conversationPort;
    private final EmotionService emotionService;

    public ConversationDomainService(ConversationPort conversationPort,
                                     EmotionService emotionService) {
        this.conversationPort = conversationPort;
        this.emotionService = emotionService;
    }

    public Flux<ConversationDomain> getConversationsByUserId(String userId) {
        return conversationPort.findByUserId(userId);
    }

    public Mono<ConversationDomain> getConversationById(Long id) {
        return conversationPort.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Conversation with ID " + id + " not found")));
    }

    public Flux<MessageDomain> getMessagesByConversationId(Long conversationId) {
        return conversationPort.findMessagesByConversationId(conversationId);
    }

    public Mono<Long> getMessageCount(Long conversationId) {
        return conversationPort.countMessagesByConversationId(conversationId);
    }

    public Mono<ConversationDomain> startConversation(String userId, String title) {
        ConversationDomain conversation = new ConversationDomain(
                null,
                userId,
                title,
                "neutral",
                LocalDateTime.now()
        );
        return conversationPort.save(conversation);
    }

    public Mono<MessageDomain> addMessage(Long conversationId, String sender, String content) {
        if (conversationId == null) {
            return Mono.error(new IllegalArgumentException("Conversation ID cannot be null"));
        }
        if (sender == null || sender.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Sender cannot be null or empty"));
        }
        if (content == null || content.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Content cannot be null or empty"));
        }

        MessageDomain message = new MessageDomain(
                null,
                conversationId,
                sender,
                content,
                LocalDateTime.now()
        );

        return conversationPort.findById(conversationId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Conversation with ID " + conversationId + " not found")))
                .flatMap(conversation -> {
                    if ("user".equalsIgnoreCase(sender)) {
                        return conversationPort.saveMessage(message)
                                .flatMap(savedMessage ->
                                        emotionService.extractEmotion(content)
                                                .flatMap(emotionResult -> {
                                                    String emotion = emotionResult.topLabel().toString();
                                                    ConversationDomain updated = conversation.withEmotion(emotion);
                                                    return conversationPort.save(updated)
                                                            .thenReturn(savedMessage);
                                                })
                                );
                    }
                    return conversationPort.saveMessage(message);
                });
    }

    public Mono<ConversationDomain> updateConversationTitle(Long conversationId, String newTitle) {
        return conversationPort.findById(conversationId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Conversation with ID " + conversationId + " not found")))
                .flatMap(conversation -> {
                    ConversationDomain updated = new ConversationDomain(
                            conversation.id(),
                            conversation.userId(),
                            newTitle,
                            conversation.emotion(),
                            conversation.createdAt()
                    );
                    return conversationPort.save(updated);
                });
    }

    public Mono<Void> deleteConversation(Long conversationId) {
        return conversationPort.findById(conversationId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Conversation with ID " + conversationId + " not found")))
                .flatMap(conversation -> conversationPort.deleteById(conversationId));
    }
}
