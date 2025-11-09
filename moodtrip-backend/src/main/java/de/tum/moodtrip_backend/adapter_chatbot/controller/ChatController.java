package de.tum.moodtrip_backend.adapter_chatbot.controller;

import de.tum.moodtrip_backend.adapter_chatbot.service.EmotionService;
import de.tum.moodtrip_backend.core.model.EmotionResult;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class ChatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);

    private final EmotionService emotionService;

    public ChatController(EmotionService emotionService) {
        this.emotionService = emotionService;
    }

    @PostMapping("/ai/extractEmotion")
    public Mono<EmotionResult> extractEmotion(@RequestParam @NotBlank String message) {
        LOGGER.info("Start extracting user emotions");
        return emotionService.extractEmotion(message)
                .doOnSuccess(e -> LOGGER.info("Successfully extracted user emotions"))
                .doOnError(err -> LOGGER.error("Error while extracting emotions", err));
    }
}
