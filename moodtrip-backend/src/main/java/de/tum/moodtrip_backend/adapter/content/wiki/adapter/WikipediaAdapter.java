package de.tum.moodtrip_backend.adapter.content.wiki.adapter;

import de.tum.moodtrip_backend.adapter.content.wiki.mapper.WikipediaMediaMapper;
import de.tum.moodtrip_backend.core.port.WikipediaPort;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Iterator;
import java.util.Map;

@Service
public class WikipediaAdapter implements WikipediaPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaAdapter.class);
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
                .doOnError(e -> LOGGER.warn("Failed to fetch Wikipedia summary for tag {}: {}", wikipediaTag, e.getMessage()))
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
                    if (url == null || url.isBlank()) {
                        return Mono.empty();
                    }
                    return resolveCommonsImageUrl(url).switchIfEmpty(Mono.just(url));
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
                    if (url == null || url.isBlank()) {
                        return Mono.empty();
                    }
                    return resolveCommonsImageUrl(url).switchIfEmpty(Mono.just(url));
                })
                .doOnError(e -> LOGGER.warn("Failed to fetch Wikidata image for ID {}: {}", id, e.getMessage()))
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
        if (isCommonsCategoryTag(trimmed)) {
            return fetchImageFromCommonsCategory(trimmed);
        }
        // Build a basic Commons page URL and delegate normalization to the mapper.
        String title = trimmed.replace(' ', '_');
        String commonsPageUrl = "https://commons.wikimedia.org/wiki/" + title;

        String normalized = WikipediaMediaMapper.normalizeCommonsFileUrl(commonsPageUrl);
        if (normalized == null || normalized.isBlank()) {
            return Mono.empty();
        }
        return resolveCommonsImageUrl(normalized).switchIfEmpty(Mono.just(normalized));
    }

    private static boolean isCommonsCategoryTag(String commonsTag) {
        String lower = commonsTag.toLowerCase();
        return lower.startsWith("category:") || lower.startsWith("kategorie:");
    }

    private Mono<String> fetchImageFromCommonsCategory(String categoryTag) {
        String title = categoryTag.trim().replace(' ', '_');
        WebClient commonsClient = buildCommonsClient();

        return commonsClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/w/api.php")
                        .queryParam("action", "query")
                        .queryParam("list", "categorymembers")
                        .queryParam("cmtitle", title)
                        .queryParam("cmtype", "file")
                        .queryParam("cmlimit", "1")
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(json -> {
                    JsonNode members = json.path("query").path("categorymembers");
                    if (!members.isArray() || members.isEmpty()) {
                        return Mono.empty();
                    }
                    String fileTitle = members.get(0).path("title").asText("");
                    if (fileTitle.isBlank()) {
                        return Mono.empty();
                    }
                    return fetchCommonsImageInfoForTitle(fileTitle)
                            .switchIfEmpty(Mono.defer(() -> {
                                String filePageUrl = "https://commons.wikimedia.org/wiki/" + fileTitle.replace(' ', '_');
                                String normalized = WikipediaMediaMapper.normalizeCommonsFileUrl(filePageUrl);
                                if (normalized == null || normalized.isBlank()) {
                                    return Mono.empty();
                                }
                                return resolveCommonsImageUrl(normalized).switchIfEmpty(Mono.just(normalized));
                            }));
                })
                .doOnError(e -> LOGGER.warn("Failed to fetch Commons category image for {}: {}", title, e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    private WebClient buildCommonsClient() {
        return wikipediaClient.mutate()
                .baseUrl("https://commons.wikimedia.org")
                .build();
    }

    private Mono<String> resolveCommonsImageUrl(String input) {
        if (input == null || input.isBlank()) {
            return Mono.empty();
        }
        if (WikipediaMediaMapper.isUploadWikimediaUrl(input)) {
            return Mono.just(input.trim());
        }
        String fileTitle = WikipediaMediaMapper.extractCommonsFileTitle(input);
        if (fileTitle == null || fileTitle.isBlank()) {
            return Mono.empty();
        }
        return fetchCommonsImageInfoForTitle(fileTitle)
                .switchIfEmpty(Mono.defer(() -> {
                    if (hasExtension(fileTitle)) {
                        return Mono.empty();
                    }
                    String prefix = stripFilePrefix(fileTitle);
                    return fetchCommonsImageInfoByPrefix(prefix);
                }));
    }

    private Mono<String> fetchCommonsImageInfoForTitle(String fileTitle) {
        if (fileTitle == null || fileTitle.isBlank()) {
            return Mono.empty();
        }
        String normalizedTitle = normalizeFileTitle(fileTitle);
        WebClient commonsClient = buildCommonsClient();
        return commonsClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/w/api.php")
                        .queryParam("action", "query")
                        .queryParam("titles", normalizedTitle)
                        .queryParam("prop", "imageinfo")
                        .queryParam("iiprop", "url")
                        .queryParam("redirects", "1")
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(json -> {
                    String url = extractImageInfoUrl(json);
                    return (url == null || url.isBlank()) ? Mono.empty() : Mono.just(url);
                })
                .doOnError(e -> LOGGER.warn("Failed to fetch Commons image info for {}: {}", normalizedTitle, e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<String> fetchCommonsImageInfoByPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return Mono.empty();
        }
        WebClient commonsClient = buildCommonsClient();
        return commonsClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/w/api.php")
                        .queryParam("action", "query")
                        .queryParam("generator", "allimages")
                        .queryParam("gaiprefix", prefix.trim().replace(' ', '_'))
                        .queryParam("gailimit", "1")
                        .queryParam("prop", "imageinfo")
                        .queryParam("iiprop", "url")
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(json -> {
                    String url = extractImageInfoUrl(json);
                    return (url == null || url.isBlank()) ? Mono.empty() : Mono.just(url);
                })
                .doOnError(e -> LOGGER.warn("Failed to fetch Commons image by prefix {}: {}", prefix, e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    private static String extractImageInfoUrl(JsonNode json) {
        if (json == null) {
            return null;
        }
        JsonNode pages = json.path("query").path("pages");
        if (!pages.isObject()) {
            return null;
        }
        // Cast to ObjectNode to avoid deprecated JsonNode.fields()
        Iterator<Map.Entry<String, JsonNode>> fields = ((com.fasterxml.jackson.databind.node.ObjectNode) pages).fields();
        while (fields.hasNext()) {
            JsonNode page = fields.next().getValue();
            JsonNode imageinfo = page.path("imageinfo");
            if (imageinfo.isArray() && !imageinfo.isEmpty()) {
                String url = imageinfo.get(0).path("url").asText("");
                if (!url.isBlank()) {
                    return url;
                }
            }
        }
        return null;
    }

    private static String normalizeFileTitle(String fileTitle) {
        String normalized = fileTitle.trim().replace(' ', '_');
        return normalized.regionMatches(true, 0, "File:", 0, "File:".length())
                ? normalized
                : "File:" + normalized;
    }

    private static boolean hasExtension(String fileTitle) {
        String name = stripFilePrefix(fileTitle);
        return name.contains(".");
    }

    private static String stripFilePrefix(String fileTitle) {
        int colon = fileTitle.indexOf(':');
        String name = colon >= 0 ? fileTitle.substring(colon + 1) : fileTitle;
        return name.trim().replace(' ', '_');
    }

    // Caches to prevent redundant network calls for the same identifier
    private final java.util.concurrent.ConcurrentMap<String, Mono<String>> summaryCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentMap<String, Mono<String>> imageCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Resolve an image URL for a POI given optional image, Wikipedia, Wikidata and Wikimedia Commons tags.
     * <p>
     * Resolution strategy:
     * <ol>
     *   <li>If an imageTag is present, try to resolve it to a direct image URL.</li>
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
        String cacheKey = String.format("img|%s|%s|%s|%s", imageTag, wikipediaTag, wikidataId, wikimediaCommonsTag);
        
        return imageCache.computeIfAbsent(cacheKey, k -> {
            Mono<String> directImage = Mono.empty();
            if (imageTag != null && !imageTag.isBlank()) {
                if (WikipediaMediaMapper.isValidHttpUrl(imageTag)) {
                    String normalized = WikipediaMediaMapper.normalizeCommonsFileUrl(imageTag);
                    directImage = resolveCommonsImageUrl(normalized).switchIfEmpty(Mono.just(normalized));
                } else {
                    directImage = resolveCommonsImageUrl(imageTag);
                }
                directImage = directImage.doOnNext(url -> LOGGER.info("Using direct image tag: {}", url));
            }

            // 2. Wikipedia → image (originalimage/thumbnail)
            return directImage.switchIfEmpty(fetchImageFromWikipediaTag(wikipediaTag)
                    .doOnNext(url -> LOGGER.info("Found image via Wikipedia tag {}: {}", wikipediaTag, url))
                    // 3. If still empty, fall back to Wikidata → P18 → Commons
                    .switchIfEmpty(fetchImageFromWikidataId(wikidataId)
                            .doOnNext(url -> LOGGER.info("Found image via Wikidata ID {}: {}", wikidataId, url)))
                    // 4. Finally, try Wikimedia Commons tag as a last resort
                    .switchIfEmpty(fetchImageFromWikimediaCommonsTag(wikimediaCommonsTag)
                            .doOnNext(url -> LOGGER.info("Found image via Commons tag {}: {}", wikimediaCommonsTag, url))))
                    .cache(); // Cache the result of the Mono itself (success or empty/error)
        });
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

        return summaryCache.computeIfAbsent(wikiTag, k -> {
            LOGGER.info("Fetching summary for Wikipedia tag: {}", wikiTag);
            return fetchSummaryFromWikipediaTag(wikiTag).cache();
        });
    }
}
