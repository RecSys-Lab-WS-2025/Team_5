package de.tum.moodtrip_backend.adapter_wiki.adapter;

import de.tum.moodtrip_backend.core.port.WikipediaPort;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class WikipediaAdapter implements WikipediaPort {

    private final WebClient wikipediaClient;

    public WikipediaAdapter(WebClient.Builder webClientBuilder) {
        this.wikipediaClient = webClientBuilder.build();
    }

    private Mono<String> fetchSummaryFromWikipediaTag(String wikipediaTag) {
        if (wikipediaTag == null || wikipediaTag.isBlank()) {
            return Mono.empty();
        }

        String[] parts = wikipediaTag.split(":", 2);
        String lang = parts.length == 2 ? parts[0] : "en";
        String title = parts.length == 2 ? parts[1] : parts[0];

        WebClient clientForLang = wikipediaClient.mutate()
                .baseUrl("https://" + lang + ".wikipedia.org/api/rest_v1")
                .build();

        return clientForLang.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/page/summary/{title}")
                        .build(title))
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> body.get("extract") instanceof String s ? s : "")
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * Fetch a short summary for a POI given its OSM tags.
     * Prefers a direct wikipedia=* tag and falls back to wikidata=* if available.
     *
     * @param wikiTag       String representing a Wikipedia tag value (e.g., "en:Article_Title")
     */
    @Override
    public Mono<String> fetchSummaryForTags(String wikiTag) {
        if (wikiTag == null || wikiTag.isBlank()) {
            return Mono.empty();
        }
        return fetchSummaryFromWikipediaTag(wikiTag);
    }
}
