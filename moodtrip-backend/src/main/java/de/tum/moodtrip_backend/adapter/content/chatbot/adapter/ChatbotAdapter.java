package de.tum.moodtrip_backend.adapter.content.chatbot.adapter;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.stereotype.Service;

import de.tum.moodtrip_backend.adapter.content.chatbot.mapper.EmotionMapper;
import de.tum.moodtrip_backend.adapter.content.chatbot.mapper.RouteTextMapper;
import de.tum.moodtrip_backend.adapter.content.chatbot.provider.ChatbotPromptProvider;
import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.EmotionResult;
import de.tum.moodtrip_backend.core.model.EnrichedPoi;
import de.tum.moodtrip_backend.core.model.RouteText;
import de.tum.moodtrip_backend.core.port.ConversationTitlePort;
import de.tum.moodtrip_backend.core.port.EmotionPort;
import de.tum.moodtrip_backend.core.port.RouteDescriptionGeneratorPort;
import reactor.core.publisher.Mono;

@Service
public class ChatbotAdapter implements EmotionPort, ConversationTitlePort, RouteDescriptionGeneratorPort {

    private final String emotionDetectionPrompt;
    private final String conversationTitlePrompt;
    private final String routeDescriptionPrompt;
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatbotAdapter.class);

    private final DeepSeekChatModel chatModel;

    public ChatbotAdapter(DeepSeekChatModel chatModel,
                          ChatbotPromptProvider chatbotPromptProvider) {
        this.chatModel = chatModel;
        this.emotionDetectionPrompt = chatbotPromptProvider.getEmotionPrompt();
        this.conversationTitlePrompt = chatbotPromptProvider.getTitlePrompt();
        this.routeDescriptionPrompt = chatbotPromptProvider.getRouteDescriptionPrompt();
    }

    @Override
    public Mono<EmotionResult> extractEmotion(String historyAndNewMessage) {
        if (historyAndNewMessage != null && historyAndNewMessage.toLowerCase().contains("mock123321")) {
            String targetEmotionName = "JOYFUL";
            if (historyAndNewMessage.contains(":")) {
                String[] parts = historyAndNewMessage.split(":");
                if (parts.length > 1) {
                    targetEmotionName = parts[parts.length-1].trim();
                }
            }

            Emotion emotion = Emotion.fromString(targetEmotionName);
            
            java.util.Map<Emotion, Double> scores = new java.util.HashMap<>();
            for (Emotion e : Emotion.values()) {
                scores.put(e, e == emotion ? 1.0 : 0.0);
            }

            EmotionResult mockResult = new EmotionResult(
                    scores,
                    emotion,
                    1.0,
                    "{THIS IS A MOCK RESPONSE} I noticed you are feeling " + emotion + "! If you answer a few questions, I can plan something wonderful for you.",
                    true
            );

            return Mono.just(mockResult);
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

    @Override
    public Mono<RouteText> generateRouteText(String mood, String city, List<EnrichedPoi> pois, boolean isMocked) {
        LOGGER.info("Generating route text for mood: {}, city: {}, with {} POIs (isMocked={})", 
            mood, city, pois != null ? pois.size() : 0, isMocked);

        // Validate inputs
        if (mood == null || mood.trim().isEmpty()) {
            LOGGER.warn("Invalid mood parameter, returning error response");
            return Mono.just(RouteTextMapper.createErrorRouteText("Invalid mood", city));
        }

        // Mock mode: Bypass LLM completely
        if (isMocked) {
            LOGGER.info("Mock mode enabled: Bypassing LLM and generating mock route text.");
            return Mono.just(RouteTextMapper.createMockRouteText(mood, city, pois));
        }

        // Normal AI generation flow
        LOGGER.info("Proceeding with LLM-based route text generation.");
        String input = buildAiInput(mood, city, pois);

        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage(routeDescriptionPrompt),
                        new UserMessage(input)
                )
        );

        return chatModel.stream(prompt)
                .mapNotNull(resp -> resp.getResult().getOutput().getText())
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .filter(result -> !result.isBlank())
                .switchIfEmpty(Mono.error(new RuntimeException("AI returned empty response for route text")))
                .map(RouteTextMapper::fromAiResponse)
                .doOnNext(routeText -> LOGGER.info("Successfully generated AI route text - Title: '{}', Description length: {} chars ",
                    routeText.title(), routeText.description().length()))
                .onErrorResume(error -> {
                    LOGGER.warn("AI generation failed ({}), falling back to mock generation as safety net.", error.getMessage());
                    return Mono.just(RouteTextMapper.createMockRouteText(mood, city, pois));
                })
                .doFinally(signalType -> LOGGER.info("Route text generation completed with signal: {}", signalType));
    }

    /**
     * Build structured input for AI processing with detailed POI information
     */
    private String buildAiInput(String mood, String city, List<EnrichedPoi> pois) {
        LOGGER.info("Building AI input with {} POIs", pois != null ? pois.size() : 0);
        
        String poisInfo = pois.stream()
                .limit(15) // Limit to avoid token overflow
                .map(poi -> {
                    String name = poi.poi().name();
                    String category = poi.poi().category().toString();
                    String description = poi.description();
                    
                    // Format: - Name (Category): Description
                    if (description != null && !description.trim().isEmpty()) {
                        String formatted = String.format("- %s (%s): %s", name, category, description);
                        LOGGER.debug("POI with description: {}", formatted);
                        return formatted;
                    } else {
                        String formatted = String.format("- %s (%s)", name, category);
                        LOGGER.debug("POI without description: {}", formatted);
                        return formatted;
                    }
                })
                .collect(Collectors.joining("\n"));

        String fullInput = String.format(
            "Mood: %s\nCity: %s\nPoints of Interest:\n%s",
            mood, city, poisInfo
        );
        
        LOGGER.info("Complete AI input:\n{}", fullInput);
        return fullInput;
    }
}