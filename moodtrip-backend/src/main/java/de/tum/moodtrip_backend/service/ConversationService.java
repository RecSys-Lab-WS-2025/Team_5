package de.tum.moodtrip_backend.service;

import de.tum.moodtrip_backend.adapter_chatbot.service.EmotionService;
import de.tum.moodtrip_backend.exception.ResourceNotFoundException;
import de.tum.moodtrip_backend.model.Conversation;
import de.tum.moodtrip_backend.model.Message;
import de.tum.moodtrip_backend.repository.ConversationRepository;
import de.tum.moodtrip_backend.repository.MessageRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final EmotionService emotionService;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository,
                               EmotionService emotionService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.emotionService = emotionService;
    }

    public Flux<Conversation> getConversationsByUserId(String userId) {
        return conversationRepository.findByUserId(userId);
    }

    public Flux<Message> getMessagesByConversationId(Long conversationId) {
        return messageRepository.findByConversationId(conversationId);
    }

    public Mono<Conversation> startConversation(String userId, String title) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(title);
        conversation.setEmotion("neutral"); // Default emotion, will be updated when first message is added
        conversation.setCreatedAt(LocalDateTime.now());
        return conversationRepository.save(conversation);
    }

    public Mono<Message> addMessage(Long conversationId, String sender, String content) {
        if (conversationId == null) {
            return Mono.error(new IllegalArgumentException("Conversation ID cannot be null"));
        }
        if (sender == null || sender.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Sender cannot be null or empty"));
        }
        if (content == null || content.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Content cannot be null or empty"));
        }

        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setSender(sender);
        msg.setContent(content);
        msg.setTimestamp(LocalDateTime.now());

        // Verify conversation exists first
        return conversationRepository.findById(conversationId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Conversation with ID " + conversationId + " not found")))
                .flatMap(conversation -> {
                    if ("user".equalsIgnoreCase(sender)) {
                        return messageRepository.save(msg)
                                .flatMap(savedMessage ->
                                        emotionService.extractEmotion(content)
                                                .flatMap(emotion -> {
                                                    conversation.setEmotion(emotion.toString());
                                                    return conversationRepository.save(conversation)
                                                            .thenReturn(savedMessage);
                                                })
                                );
                    }
                    return messageRepository.save(msg);
                });
    }
}
