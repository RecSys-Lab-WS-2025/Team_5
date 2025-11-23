package de.tum.moodtrip_backend.core.port;

import reactor.core.publisher.Mono;

public interface WikipediaPort {
    Mono<String> fetchSummaryForTags(String wikiTag);
}
