package de.tum.moodtrip_backend.adapter.content.chatbot.adapter;

import de.tum.moodtrip_backend.adapter.content.chatbot.mapper.EmotionMapper;
import de.tum.moodtrip_backend.adapter.content.chatbot.provider.ChatbotPromptProvider;
import de.tum.moodtrip_backend.core.model.EmotionResult;
import de.tum.moodtrip_backend.core.port.ConversationTitlePort;
import de.tum.moodtrip_backend.core.port.EmotionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;


@Service
public class ChatbotAdapter implements EmotionPort, ConversationTitlePort {

    private final String emotionDetectionPrompt;
    private final String conversationTitlePrompt;
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatbotAdapter.class);

    private final DeepSeekChatModel chatModel;

    public ChatbotAdapter(DeepSeekChatModel chatModel,
                          ChatbotPromptProvider chatbotPromptProvider) {
        this.chatModel = chatModel;
        this.emotionDetectionPrompt = chatbotPromptProvider.getEmotionPrompt();
        this.conversationTitlePrompt = chatbotPromptProvider.getTitlePrompt();
    }

    @Override
    public Mono<EmotionResult> extractEmotion(String historyAndNewMessage) {
        if (historyAndNewMessage != null && historyAndNewMessage.toLowerCase().contains("mock123321")) {
            // Mock response JSON structure must match what EmotionMapper expects (assuming standard JSON format)
            String mockJson = """
                    {"scores":{"JOYFUL":0.900,"ENERGIZED":0.050,"CALM":0.010,"CONTENT":0.010,"HOPEFUL":0.010,"GRATEFUL":0.010,"CURIOUS":0.000,"NOSTALGIC":0.000,"NEUTRAL":0.000,"CONFUSED":0.000,"BORED":0.000,"TIRED":0.000,"LONELY":0.000,"SAD":0.000,"ANXIOUS":0.000,"STRESSED":0.000,"FRUSTRATED":0.000,"ANGRY":0.000,"OVERWHELMED":0.000},
                    "top_label":"JOYFUL",
                    "top_score":0.900,
                    "success":true,
                    "content":"I noticed you are feeling joyful! If you answer a few questions, I can plan something wonderful for you."
                    }
                    """;

            return Mono.just(EmotionMapper.fromJson(mockJson));
        }

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
                .doOnNext(LOGGER::info)
                .switchIfEmpty(Mono.error(new RuntimeException("AI returned empty response")))
                .map(EmotionMapper::fromJson);
    }

    @Override
    public Mono<String> generateConversationTitle(String transcript) {
        if (transcript != null && transcript.toLowerCase().contains("mock123321")) {
            return Mono.just("Mock Conversation Title");
        }

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
}