package de.tum.moodtrip_backend.controller;


import de.tum.moodtrip_backend.service.EmotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class EmotionController {
    @Autowired
    private EmotionService emotionService;

    @Autowired
    public EmotionController(EmotionService emotionService) {
        this.emotionService = emotionService;
    }

    @GetMapping("/emotion/analyze")
    public Mono<String> getEmotion(@RequestParam String input) {
        return emotionService.analyzeEmotion(input);
    }
}
