package de.tum.moodtrip_backend.core.service;

import java.time.LocalDateTime;

import de.tum.moodtrip_backend.core.model.ConversationDomain;
import de.tum.moodtrip_backend.core.model.MessageDomain;
import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.Sender;
import de.tum.moodtrip_backend.core.model.EmotionResult;
import de.tum.moodtrip_backend.core.port.ConversationTitlePort;
import de.tum.moodtrip_backend.core.port.ConversationPort;
import de.tum.moodtrip_backend.core.port.EmotionPort;
import de.tum.moodtrip_backend.exception.ResourceNotFoundException;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ConversationDomainService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationDomainService.class);

    private final ConversationPort conversationPort;
    private final EmotionPort emotionPort;
    private final ConversationTitlePort conversationTitlePort;

    public ConversationDomainService(ConversationPort conversationPort,
                                     EmotionPort emotionPort,
                                     ConversationTitlePort conversationTitlePort) {
        this.conversationPort = conversationPort;
        this.emotionPort = emotionPort;
        this.conversationTitlePort = conversationTitlePort;
    }

    /**
     * Get all conversations for a given user.
     */
    public Flux<ConversationDomain> getConversationsByUserId(Long userId) {
        return conversationPort.findByUserId(userId);
    }

    /**
     * Get a single conversation by its ID.
     * Throws ResourceNotFoundException if no conversation exists.
     */
    public Mono<ConversationDomain> getConversationById(Long id) {
        return conversationPort.findById(id)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Conversation with ID " + id + " not found")
                ));
    }

    /**
     * Get all messages of a conversation.
     * NOTE: This method does not perform any permission checks.
     * Caller is responsible for validating conversation ownership if needed.
     */
    public Flux<MessageDomain> getMessagesByConversationId(Long conversationId) {
        return conversationPort.findMessagesByConversationId(conversationId);
    }

    /**
     * Count messages of a conversation.
     */
    public Mono<Long> getMessageCount(Long conversationId) {
        return conversationPort.countMessagesByConversationId(conversationId);
    }

    /**
     * Start a new conversation for the given user with an initial title.
     */
    public Mono<ConversationDomain> startConversation(Long userId, String title) {
        LOGGER.info("Starting new conversation for user identified by id {} with title: {}", userId, title);
        ConversationDomain conversation = new ConversationDomain(
                null,
                userId,
                title,
                Emotion.NEUTRAL,
                LocalDateTime.now()
        );
        return conversationPort.save(conversation)
                .doOnSuccess(c -> LOGGER.info("Conversation started with ID: {}", c.id()));
    }

    /**
     * Update conversation title without user check (for internal usage if needed).
     */
    public Mono<ConversationDomain> updateConversationTitle(Long conversationId, String newTitle) {
        LOGGER.info("Updating title for conversation {} to: {}", conversationId, newTitle);
        return conversationPort.findById(conversationId)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Conversation with ID " + conversationId + " not found")
                ))
                .flatMap(conversation -> {
                    ConversationDomain updated = conversation.withTitle(newTitle);
                    return conversationPort.save(updated);
                });
    }

    /**
     * Update conversation title and ensure the conversation belongs to the given user.
     * This method is intended for public API usage where user ownership must be enforced.
     */
    public Mono<ConversationDomain> updateConversationTitle(Long conversationId,
                                                            Long userId,
                                                            String newTitle) {
        return getConversationById(conversationId)
                .flatMap(conversation -> {
                    if (!conversation.userId().equals(userId)) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Access denied: This conversation does not belong to you"
                        ));
                    }
                    ConversationDomain updated = conversation.withTitle(newTitle);
                    return conversationPort.save(updated);
                });
    }

    /**
     * Delete conversation with user ownership check.
     * Messages are deleted first to avoid foreign key constraint errors.
     */
    public Mono<Void> deleteConversation(Long conversationId, Long userId) {
        return getConversationById(conversationId)
                .flatMap(conversation -> {
                    if (!conversation.userId().equals(userId)) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Access denied: This conversation does not belong to you"
                        ));
                    }

                    // First delete all messages belonging to this conversation,
                    // then delete the conversation itself.
                    return conversationPort.deleteMessagesByConversationId(conversationId)
                            .then(conversationPort.deleteById(conversationId));
                });
    }

    /**
     * Extract the user's emotion based on the latest message and conversation history.
     * Saves the user message, emotion result, updates conversation emotion,
     * and auto-generates a conversation title.
     */
    public Mono<EmotionResult> extractEmotion(Long conversationId, Long userId, String message) {
        LOGGER.info("Extracting emotion for conversation {}, message length: {}", conversationId, message != null ? message.length() : 0);
        return getConversationById(conversationId)
                .flatMap(conversation -> {
                    if (!conversation.userId().equals(userId)) {
                        LOGGER.warn("Access denied for emotion extraction. User {} on conversation {}", userId, conversationId);
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Access denied: This conversation does not belong to you"
                        ));
                    }

                    return buildConversationHistory(conversation.id(), message)
                            .flatMap(historyAndNewMessage ->
                                    saveUserMessage(conversation.id(), message)
                                            .then(emotionPort.extractEmotion(historyAndNewMessage))
                                            .flatMap(emotionResult -> {
                                                LOGGER.info("Emotion extracted: {}, Score: {}", emotionResult.topLabel(), emotionResult.topScore());
                                                return saveEmotionResult(conversationId, emotionResult)
                                                        .then(updateConversationEmotion(conversation, emotionResult))
                                                        .then(generateConversationTitle(conversationId, userId))
                                                        .thenReturn(emotionResult);
                                            })
                            );
                });
    }

    /**
     * Generate a title for the conversation using the LLM based on the transcript.
     * Ensures that the conversation belongs to the given user.
     */
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
     * Only USER messages are included in the history, each prefixed with the sender.
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
     * Persist the latest user message into the conversation.
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
     * Persist the emotion result as a bot message.
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
     * Update the stored emotion of the conversation based on the LLM result.
     */
    private Mono<Void> updateConversationEmotion(ConversationDomain conversation,
                                                 EmotionResult emotionResult) {
        String topLabel = emotionResult.topLabel().toString();
        Emotion emotion = Emotion.fromString(topLabel);
        ConversationDomain updated = conversation.withEmotion(emotion);
        return conversationPort.save(updated).then();
    }

    /**
     * Build the full conversation transcript for title generation.
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
     * Update the conversation title with the given value.
     * This private helper is used when the title has already been generated.
     */
    private Mono<Void> updateConversationTitle(ConversationDomain conversation, String title) {
        ConversationDomain updated = conversation.withTitle(title);
        return conversationPort.save(updated).then();
    }

    /**
     * Add a message (user or bot) to the conversation.
     * This method does not perform ownership checks; the caller is responsible for that.
     */
    public Mono<MessageDomain> addMessage(
            @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            String content,
            boolean isUser
    ) {
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
