package de.tum.moodtrip_backend.adapter.content.chatbot.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class ChatbotPromptProvider {

    private final String emotionPrompt;
    private final String titlePrompt;

    public ChatbotPromptProvider(
            @Value("classpath:prompts/emotion-detection.txt")
            Resource emotionPromptResource,
            @Value("classpath:prompts/title-generation.txt")
            Resource titlePromptResource
    ) throws IOException {
        this.emotionPrompt = readResource(emotionPromptResource);
        this.titlePrompt = readResource(titlePromptResource);
    }

    private String readResource(Resource resource) throws IOException {
        byte[] bytes = resource.getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public String getEmotionPrompt() {
        return emotionPrompt;
    }

    public String getTitlePrompt() {
        return titlePrompt;
    }
}