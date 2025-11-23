package de.tum.moodtrip_backend.core.port;

import reactor.core.publisher.Mono;

public interface WikipediaPort {
    Mono<String> fetchSummaryForTag(String wikiTag);

    Mono<String> fetchImageUrl(String imageTag, String wikipediaTag, String wikidataId, String wikimediaCommonsTag);

}
