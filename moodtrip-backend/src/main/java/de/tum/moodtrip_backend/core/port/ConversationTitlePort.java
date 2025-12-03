package de.tum.moodtrip_backend.core.port;

import reactor.core.publisher.Mono;

public interface ConversationTitlePort {
    Mono<String> generateConversationTitle(String transcript);
}