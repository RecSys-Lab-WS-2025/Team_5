package de.tum.moodtrip_backend.adapter_database.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.tum.moodtrip_backend.core.model.MusicRecommendationDomain;
import de.tum.moodtrip_backend.core.port.MusicRecommendationPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/test/music-recommendations")
public class MusicRecommendationTestController {

    private final MusicRecommendationPort musicRecommendationPort;

    public MusicRecommendationTestController(MusicRecommendationPort musicRecommendationPort) {
        this.musicRecommendationPort = musicRecommendationPort;
    }

    @PostMapping
    public Mono<MusicRecommendationDomain> create(@RequestBody CreateMusicRecommendationRequest request) {
        MusicRecommendationDomain domain = new MusicRecommendationDomain(
            null,
            request.conversationId(),
            request.title(),
            request.link(),
            null
        );
        return musicRecommendationPort.save(domain);
    }

    @GetMapping("/{id}")
    public Mono<MusicRecommendationDomain> getById(@PathVariable Long id) {
        return musicRecommendationPort.findById(id);
    }

    @GetMapping("/conversation/{conversationId}")
    public Flux<MusicRecommendationDomain> getByConversationId(@PathVariable Long conversationId) {
        return musicRecommendationPort.findByConversationId(conversationId);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable Long id) {
        return musicRecommendationPort.deleteById(id);
    }

    public record CreateMusicRecommendationRequest(
        Long conversationId,
        String title,
        String link
    ) {}
}
