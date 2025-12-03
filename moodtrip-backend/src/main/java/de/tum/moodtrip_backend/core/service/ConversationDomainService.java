package de.tum.moodtrip_backend.core.service;

import java.time.LocalDateTime;

import de.tum.moodtrip_backend.core.model.ConversationDomain;
import de.tum.moodtrip_backend.core.model.MessageDomain;
import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.Sender;
import de.tum.moodtrip_backend.core.model.EmotionResult;
import de.tum.moodtrip_backend.core.port.ConversationTitlePort;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import de.tum.moodtrip_backend.core.port.ConversationPort;
import de.tum.moodtrip_backend.core.port.EmotionPort;
import de.tum.moodtrip_backend.exception.ResourceNotFoundException;
import org.springframework.web.server.ResponseStatusException;
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
        return getConversationById(conversationId)
                .flatMap(conversation -> {
                    if (!conversation.userId().equals(userId)) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Access denied: This conversation does not belong to you"
                        ));
                    }

                    return buildConversationHistory(conversation.id(), message)
                            .flatMap(historyAndNewMessage ->
                                    saveUserMessage(conversation.id(), message)
                                            .then(emotionPort.extractEmotion(historyAndNewMessage))
                                            .flatMap(emotionResult ->
                                                    saveEmotionResult(conversationId, emotionResult)
                                                            .then(updateConversationEmotion(conversation, emotionResult))
                                                            .then(generateConversationTitle(conversationId,userId))
                                                            .thenReturn(emotionResult)
                                            )
                            );
                });
    }

    public Mono<String> generateConversationTitle(Long conversationId, Long userId) {
        return getConversationById(conversationId)
                .flatMap(conversation -> {
                    if (!conversation.userId().equals(userId)) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Access denied: This conversation does not belong to you"
                        ));
                    }
                    return buildConversationTranscriptForTitle(conversation.id())
                            .flatMap(transcript ->
                                    conversationTitlePort.generateConversationTitle(transcript)
                                            .flatMap(title ->
                                                    updateConversationTitle(conversation, title)
                                                            .thenReturn(title)
                                            )
                            );
                });
    }

    /**
     * Build a textual representation of the conversation history plus the latest user message.
     * Each past message is prefixed with the sender (USER/BOT).
     */
    private Mono<String> buildConversationHistory(Long conversationId, String latestMessage) {
        return conversationPort.findMessagesByConversationId(conversationId)
                .filter(messageDomain -> messageDomain.sender() == Sender.USER)
                .map(msg -> msg.sender().name() + ": " + msg.content())
                .collectList()
                .map(messages -> {
                    StringBuilder sb = new StringBuilder();
                    for (String m : messages) {
                        sb.append(m).append("\n");
                    }
                    sb.append("LATEST_MESSAGE: ").append(latestMessage);
                    return sb.toString();
                });
    }

    /**
     * Persist the latest user message in the conversation.
     */
    private Mono<MessageDomain> saveUserMessage(Long conversationId, String message) {
        MessageDomain userMessage = new MessageDomain(
                null,
                conversationId,
                Sender.USER,
                message,
                LocalDateTime.now()
        );
        return conversationPort.saveMessage(userMessage);
    }

    /**
     * Persist the bot's emotion result as a message in the conversation.
     */
    private Mono<MessageDomain> saveEmotionResult(Long conversationId, EmotionResult emotionResult) {
        MessageDomain botMessage = new MessageDomain(
                null,
                conversationId,
                Sender.BOT,
                emotionResult.toString(),
                LocalDateTime.now()
        );
        return conversationPort.saveMessage(botMessage);
    }

    /**
     * Update the conversation's stored emotion based on the LLM's result.
     */
    private Mono<Void> updateConversationEmotion(ConversationDomain conversation,
                                                 EmotionResult emotionResult) {
        String topLabel = emotionResult.topLabel().toString();
        Emotion emotion = Emotion.fromString(topLabel);
        ConversationDomain updated = conversation.withEmotion(emotion);
        return conversationPort.save(updated).then();
    }

    /**
     * Build a textual representation of the full conversation history for title generation.
     * Includes both USER and BOT messages, each prefixed with the sender.
     */
    private Mono<String> buildConversationTranscriptForTitle(Long conversationId) {
        return conversationPort.findMessagesByConversationId(conversationId)
                .map(msg -> msg.sender().name() + ": " + msg.content())
                .collectList()
                .map(lines -> String.join("\n", lines))
                .filter(transcript -> !transcript.isBlank())
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot generate title for conversation with no messages"
                )));
    }

    /**
     * Update the conversation's stored title based on the LLM's result.
     */
    private Mono<Void> updateConversationTitle(ConversationDomain conversation, String title) {
        ConversationDomain updated = conversation.withTitle(title);
        return conversationPort.save(updated).then();
    }


    public Mono<MessageDomain> addMessage(@NotNull(message = "Conversation ID cannot be null") Long conversationId, String content, boolean isUser) {
        MessageDomain messageDomain = new MessageDomain(
                null,
                conversationId,
                isUser ? Sender.USER : Sender.BOT,
                content,
                LocalDateTime.now()
        );
        return conversationPort.saveMessage(messageDomain);

    }
}
