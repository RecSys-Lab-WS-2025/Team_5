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
import de.tum.moodtrip_backend.core.model.RouteGenerationContext;
import de.tum.moodtrip_backend.core.model.RouteType;
import de.tum.moodtrip_backend.core.port.RouteDescriptionGeneratorPort;
import reactor.core.publisher.Mono;

@Service
public class ChatbotAdapter implements EmotionPort, ConversationTitlePort, RouteDescriptionGeneratorPort {

    private final String emotionDetectionPrompt;
    private final String conversationTitlePrompt;

    private final String batchRouteDescriptionPrompt;
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatbotAdapter.class);

    private final DeepSeekChatModel chatModel;

    public ChatbotAdapter(DeepSeekChatModel chatModel,
                          ChatbotPromptProvider chatbotPromptProvider) {
        this.chatModel = chatModel;
        this.emotionDetectionPrompt = chatbotPromptProvider.getEmotionPrompt();
        this.conversationTitlePrompt = chatbotPromptProvider.getTitlePrompt();
        this.batchRouteDescriptionPrompt = chatbotPromptProvider.getBatchRouteDescriptionPrompt();
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
                .map(EmotionMapper::fromJson)
                .onErrorResume(e -> {
                    LOGGER.error("Emotion extraction failed: {}", e.getMessage());
                    // Return a default failure result so the conversation flow continues
                    return Mono.just(new EmotionResult(
                        java.util.Collections.emptyMap(),
                        Emotion.NEUTRAL,
                        0.0,
                        "I'm sorry, I am currently having trouble connecting to my brain. Could you please try again?",
                        false
                    ));
                });
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
    public Mono<java.util.Map<RouteType, RouteText>> generateBatchRouteText(String mood, String city, List<RouteGenerationContext> contexts, boolean isMocked) {
        LOGGER.info("Generating BATCH route text for mood: {}, city: {}, with {} contexts (isMocked={})",
            mood, city, contexts != null ? contexts.size() : 0, isMocked);
            
        if (contexts == null || contexts.isEmpty()) {
            return Mono.just(java.util.Map.of());
        }

        // Mock mode: Bypass LLM
        if (isMocked) {
             LOGGER.info("Mock mode enabled: Generating mock batch route text.");
             java.util.Map<RouteType, RouteText> mockResults = new java.util.HashMap<>();
             for (RouteGenerationContext ctx : contexts) {
                 mockResults.put(ctx.routeType(), RouteTextMapper.createMockRouteText(mood, city, ctx.pois(), ctx.tripDays()));
             }
             return Mono.just(mockResults);
        }

        // Normal AI generation flow
        String input = buildBatchAiInput(mood, city, contexts);
        LOGGER.debug("Batch AI Prompt: {}", input);

        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage(batchRouteDescriptionPrompt),
                        new UserMessage(input)
                )
        );

        return chatModel.stream(prompt)
                .mapNotNull(resp -> resp.getResult().getOutput().getText())
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .filter(result -> !result.isBlank())
                .switchIfEmpty(Mono.error(new RuntimeException("AI returned empty response for batch route text")))
                .doOnNext(response -> LOGGER.debug("Batch AI Response: {}", response))
                .map(RouteTextMapper::parseBatchResponse)
                .doOnNext(results -> LOGGER.info("Successfully generated AI batch route text for {} routes", results.size()))
                .onErrorResume(error -> {
                    LOGGER.warn("Batch AI generation failed ({}), falling back to mock generation.", error.getMessage());
                    java.util.Map<RouteType, RouteText> mockResults = new java.util.HashMap<>();
                    for (RouteGenerationContext ctx : contexts) {
                        mockResults.put(ctx.routeType(), RouteTextMapper.createMockRouteText(mood, city, ctx.pois(), ctx.tripDays()));
                    }
                    return Mono.just(mockResults);
                });
    }

    private String buildBatchAiInput(String mood, String city, List<RouteGenerationContext> contexts) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Mood: %s\nCity: %s\n\n", mood, city));
        
        for (RouteGenerationContext ctx : contexts) {
            sb.append("Route Context: ").append(ctx.routeType()).append("\n");
            sb.append("Trip Days: ").append(ctx.tripDays()).append("\n");
            
            List<EnrichedPoi> effectivePois = (ctx.pois() != null) ? ctx.pois() : List.of();
            int totalPois = Math.min(effectivePois.size(), 15);
            
            for (int day = 1; day <= ctx.tripDays(); day++) {
                sb.append(String.format("Day %d Points of Interest:\n", day));
                final int currentDay = day;
                for (int i = 0; i < totalPois; i++) {
                     int poiDay = (int) Math.floor((double) i * ctx.tripDays() / totalPois) + 1;
                     if (poiDay == currentDay) {
                         EnrichedPoi poi = effectivePois.get(i);
                         String name = poi.poi().name();
                         String category = poi.poi().category().toString();
                         sb.append(String.format("- %s (%s)\n", name, category));
                     }
                }
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }


}