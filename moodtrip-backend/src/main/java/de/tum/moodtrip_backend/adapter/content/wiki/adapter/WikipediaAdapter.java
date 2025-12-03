package de.tum.moodtrip_backend.adapter.content.wiki.adapter;

import de.tum.moodtrip_backend.adapter.content.wiki.mapper.WikipediaMediaMapper;
import de.tum.moodtrip_backend.core.port.WikipediaPort;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class WikipediaAdapter implements WikipediaPort {

    private final WebClient wikipediaClient;

    public WikipediaAdapter(WebClient.Builder webClientBuilder) {
        this.wikipediaClient = webClientBuilder.build();
    }


    private record WikipediaTag(String lang, String title) {
    }

    private WikipediaTag parseWikipediaTag(String wikipediaTag) {
        String[] parts = wikipediaTag.split(":", 2);
        String lang = parts.length == 2 ? parts[0] : "en";
        String title = parts.length == 2 ? parts[1] : parts[0];
        return new WikipediaTag(lang, title);
    }

    private WebClient buildWikipediaClientForLang(String lang) {
        return wikipediaClient.mutate()
                .baseUrl("https://" + lang + ".wikipedia.org/api/rest_v1")
                .build();
    }

    private <T> Mono<T> fetchWikipediaSummary(String wikipediaTag, Class<T> bodyType) {
        if (wikipediaTag == null || wikipediaTag.isBlank()) {
            return Mono.empty();
        }

        WikipediaTag tag = parseWikipediaTag(wikipediaTag);
        WebClient clientForLang = buildWikipediaClientForLang(tag.lang());

        return clientForLang.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/page/summary/{title}")
                        .build(tag.title()))
                .retrieve()
                .bodyToMono(bodyType)
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<String> fetchSummaryFromWikipediaTag(String wikipediaTag) {
        return fetchWikipediaSummary(wikipediaTag, JsonNode.class)
                .map(json -> json.path("extract").asText(""));
    }

    /**
     * Fetch an image URL from a Wikipedia tag using the REST API.
     */
    private Mono<String> fetchImageFromWikipediaTag(String wikipediaTag) {
        return fetchWikipediaSummary(wikipediaTag, JsonNode.class)
                .flatMap(json -> {
                    String url = WikipediaMediaMapper.mapImageFromWikipediaJson(json);
                    return url != null ? Mono.just(url) : Mono.empty();
                });
    }

    /**
     * Fetch an image URL from a Wikidata Q-ID using the Special:EntityData JSON and P18.
     */
    private Mono<String> fetchImageFromWikidataId(String wikidataId) {
        if (wikidataId == null || wikidataId.isBlank()) {
            return Mono.empty();
        }

        String id = wikidataId.trim();
        if (!id.startsWith("Q")) {
            return Mono.empty();
        }

        WebClient wikidataClient = wikipediaClient.mutate()
                .baseUrl("https://www.wikidata.org")
                .build();

        String path = "/wiki/Special:EntityData/" + id + ".json";

        return wikidataClient.get()
                .uri(path)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(json -> {
                    String url = WikipediaMediaMapper.mapImageFromWikidataJson(json, id);
                    return url != null ? Mono.just(url) : Mono.empty();
                })
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * Fetch an image URL from a Wikimedia Commons tag.
     * <p>
     * The tag is typically one of:
     * <ul>
     *   <li>"File:Some_Image.jpg"</li>
     *   <li>"Category:Some Category"</li>
     *   <li>"Some page title"</li>
     * </ul>
     * For now we simply construct a Commons page URL and let the media mapper normalize it
     * to a direct file URL if possible.
     */
    private Mono<String> fetchImageFromWikimediaCommonsTag(String commonsTag) {
        if (commonsTag == null || commonsTag.isBlank()) {
            return Mono.empty();
        }

        String trimmed = commonsTag.trim();
        // Build a basic Commons page URL and delegate normalization to the mapper.
        String title = trimmed.replace(' ', '_');
        String commonsPageUrl = "https://commons.wikimedia.org/wiki/" + title;

        String normalized = WikipediaMediaMapper.normalizeCommonsFileUrl(commonsPageUrl);
        return Mono.just(normalized);
    }

    /**
     * Resolve an image URL for a POI given optional image, Wikipedia, Wikidata and Wikimedia Commons tags.
     * <p>
     * Resolution strategy:
     * <ol>
     *   <li>If a valid HTTP(S) imageTag is present, return it (normalizing Commons file-page URLs).</li>
     *   <li>Otherwise, try to fetch an image URL from the Wikipedia tag.</li>
     *   <li>Otherwise, try to fetch an image URL from the Wikidata entity (P18).</li>
     *   <li>Otherwise, try to derive an image URL from the wikimedia_commons tag.</li>
     * </ol>
     *
     * @param imageTag            value of the OSM image=* tag, may be null
     * @param wikipediaTag        value of the OSM wikipedia=* tag, e.g. "de:Mariensäule (München)"
     * @param wikidataId          value of the OSM wikidata=* tag, e.g. "Q824403"
     * @param wikimediaCommonsTag value of the OSM wikimedia_commons=* tag, e.g. "Category:Kino Intimes (Berlin)"
     * @return Mono emitting the resolved image URL or completing empty if none can be found
     */
    @Override
    public Mono<String> fetchImageUrl(String imageTag, String wikipediaTag, String wikidataId, String wikimediaCommonsTag) {
        // 1. Direct image tag, if it looks like a usable URL
        if (imageTag != null && !imageTag.isBlank() && WikipediaMediaMapper.isValidHttpUrl(imageTag)) {
            String normalized = WikipediaMediaMapper.normalizeCommonsFileUrl(imageTag);
            return Mono.just(normalized);
        }

        // 2. Wikipedia → image (originalimage/thumbnail)
        return fetchImageFromWikipediaTag(wikipediaTag)
                // 3. If still empty, fall back to Wikidata → P18 → Commons
                .switchIfEmpty(fetchImageFromWikidataId(wikidataId))
                // 4. Finally, try Wikimedia Commons tag as a last resort
                .switchIfEmpty(fetchImageFromWikimediaCommonsTag(wikimediaCommonsTag));
    }

    /**
     * Fetch a short summary for a POI given its OSM tags.
     * Prefers a direct wikipedia=* tag and falls back to wikidata=* if available.
     *
     * @param wikiTag String representing a Wikipedia tag value (e.g., "en:Article_Title")
     */
    @Override
    public Mono<String> fetchSummaryForTag(String wikiTag) {
        if (wikiTag == null || wikiTag.isBlank()) {
            return Mono.empty();
        }
        return fetchSummaryFromWikipediaTag(wikiTag);
    }
}
