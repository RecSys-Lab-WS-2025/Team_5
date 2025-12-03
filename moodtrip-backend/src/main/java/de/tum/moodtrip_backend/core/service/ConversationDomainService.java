package de.tum.moodtrip_backend.core.service;

import java.time.LocalDateTime;

import de.tum.moodtrip_backend.core.port.ConversationTitlePort;
import org.springframework.stereotype.Service;

import de.tum.moodtrip_backend.core.model.ConversationDomain;
import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.EmotionResult;
import de.tum.moodtrip_backend.core.model.MessageDomain;
import de.tum.moodtrip_backend.core.port.ConversationPort;
import de.tum.moodtrip_backend.core.port.EmotionPort;
import de.tum.moodtrip_backend.exception.ResourceNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ConversationDomainService {

    private final ConversationPort conversationPort;
    private final EmotionPort emotionPort;
    private final ConversationTitlePort conversationTitlePort;

    public ConversationDomainService(ConversationPort conversationPort,
                                     EmotionPort emotionPort, ConversationTitlePort conversationTitlePort) {
        this.conversationPort = conversationPort;
        this.emotionPort = emotionPort;
        this.conversationTitlePort = conversationTitlePort;
    }

    public Flux<ConversationDomain> getConversationsByUserId(Long userId) {
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

    public Mono<ConversationDomain> startConversation(Long userId, String title) {
        ConversationDomain conversation = new ConversationDomain(
                null,
                userId,
                title,
                Emotion.NEUTRAL,
                LocalDateTime.now()
        );
        return conversationPort.save(conversation);
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

    public Mono<EmotionResult> extractEmotion(Long conversationId, Long userId, String message) {
        return emotionPort.extractEmotion(conversationId, userId, message);
    }

    public Mono<String> generateConversationTitle(Long conversationId, Long userId) {
        return conversationTitlePort.generateConversationTitle(conversationId, userId);
    }
}
