package de.tum.moodtrip_backend.core.model;

import java.time.LocalDateTime;

public record MessageDomain(
        Long id,
        Long conversationId,
        Sender sender,
        String content,
        LocalDateTime timestamp
) {
    public MessageDomain withId(Long id) {
        return new MessageDomain(id, conversationId, sender, content, timestamp);
    }
}
