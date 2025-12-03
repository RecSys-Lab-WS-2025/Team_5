package de.tum.moodtrip_backend.adapter.content.chatbot.adapter;

import de.tum.moodtrip_backend.adapter.content.chatbot.mapper.EmotionMapper;
import de.tum.moodtrip_backend.adapter.content.chatbot.provider.ChatbotPromptProvider;
import de.tum.moodtrip_backend.core.model.*;
import de.tum.moodtrip_backend.core.port.ConversationPort;
import de.tum.moodtrip_backend.core.port.ConversationTitlePort;
import de.tum.moodtrip_backend.core.port.EmotionPort;
import de.tum.moodtrip_backend.exception.ResourceNotFoundException;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatbotAdapter implements EmotionPort, ConversationTitlePort {

    private final String emotionDetectionPrompt;
    private final String conversationTitlePrompt;

    private final DeepSeekChatModel chatModel;
    private final ConversationPort conversationPort;

    public ChatbotAdapter(DeepSeekChatModel chatModel, ConversationPort conversationPort, ChatbotPromptProvider chatbotPromptProvider) {
        this.chatModel = chatModel;
        this.conversationPort = conversationPort;
        emotionDetectionPrompt = chatbotPromptProvider.getEmotionPrompt();
        conversationTitlePrompt = chatbotPromptProvider.getTitlePrompt();
    }

    @Override
    public Mono<EmotionResult> extractEmotion(Long conversationId, Long userId, String message) {
        return getConversation(conversationId, userId)
                .flatMap(conversation ->
                        buildConversationHistory(conversation.id(), message)
                                .flatMap(historyAndNewMessage ->
                                        saveUserMessage(conversation.id(), message)
                                                .then(analyzeEmotionAndSaveBotReply(conversation, historyAndNewMessage))
                                )
                );
    }


    @Override
    public Mono<String> generateConversationTitle(Long conversationId, Long userId) {
        return getConversation(conversationId, userId)
                .flatMap(conversation ->
                        buildConversationTranscriptForTitle(conversation.id())
                                .flatMap(transcript ->
                                        generateTitleFromTranscript(transcript)
                                                .flatMap(title ->
                                                        updateConversationTitle(conversation, title)
                                                                .thenReturn(title)
                                                )
                                )
                );
    }

    /**
     * Build a textual representation of the full conversation history for title generation.
     * Includes both USER and BOT messages, each prefixed with the sender.
     */
    private Mono<String> buildConversationTranscriptForTitle(Long conversationId) {
        return conversationPort.findMessagesByConversationId(conversationId)
                .map(msg -> msg.sender().name() + ": " + msg.content())
                .collectList()
                .map(lines -> String.join("\n", lines));
    }

    /**
     * Call the LLM to generate a concise title based on the conversation transcript.
     */
    private Mono<String> generateTitleFromTranscript(String transcript) {
        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage(conversationTitlePrompt),
                        new UserMessage(transcript)
                )
        );

        return chatModel.stream(prompt)
                .mapNotNull(resp -> resp.getResult().getOutput().getText())
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .map(String::trim)
                .filter(title -> !title.isBlank())
                .switchIfEmpty(Mono.error(new RuntimeException("AI returned empty conversation title")));
    }

    /**
     * Ensure a conversation row exists for the given conversationId and return it.
     * If no such conversation exists yet, create a new one for the user.
     */
    private Mono<ConversationDomain> getConversation(Long conversationId, Long userId) {
        if (conversationId == null) {
            ConversationDomain newConversation = new ConversationDomain(
                    null,
                    userId,
                    "New Conversation",
                    Emotion.NEUTRAL,
                    LocalDateTime.now()
            );
            return conversationPort.save(newConversation);
        }

        return conversationPort.findById(conversationId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Unknown Conversation ID")));
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
     * Call the LLM to analyze emotion based on the combined history string
     * and store the bot's emotion result as a message.
     */
    private Mono<EmotionResult> analyzeEmotionAndSaveBotReply(ConversationDomain conversation,
                                                              String historyAndNewMessage) {
        Long conversationId = conversation.id();

        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage(emotionDetectionPrompt),
                        new UserMessage(historyAndNewMessage)
                )
        );

        return chatModel.stream(prompt)
                .mapNotNull(resp -> resp.getResult().getOutput().getText())
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .filter(result -> !result.isBlank())
                .switchIfEmpty(Mono.error(new RuntimeException("AI returned empty response")))
                .map(EmotionMapper::fromJson)
                .flatMap(emotionResult ->
                        saveBotMessage(conversationId, emotionResult)
                                .then(updateConversationEmotion(conversation, emotionResult))
                                .thenReturn(emotionResult)
                );
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
     * Update the conversation's stored title based on the LLM's result.
     */
    private Mono<Void> updateConversationTitle(ConversationDomain conversation, String title) {
        ConversationDomain updated = conversation.withTitle(title);
        return conversationPort.save(updated).then();
    }

    /**
     * Persist the bot's emotion result as a message in the conversation.
     */
    private Mono<MessageDomain> saveBotMessage(Long conversationId, EmotionResult emotionResult) {
        MessageDomain botMessage = new MessageDomain(
                null,
                conversationId,
                Sender.BOT,
                emotionResult.toString(),
                LocalDateTime.now()
        );
        return conversationPort.saveMessage(botMessage);
    }
}