package com.flaviodutra.notification.domain.model;

import java.util.Map;

public record MessageToSend(String to, String from, String body, String contentSid,
                            Map<String, String> contentVariables) {
    public MessageToSend {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Message requires a destination");
        }
        boolean hasBody = body != null && !body.isBlank();
        boolean hasTemplate = contentSid != null && !contentSid.isBlank();
        if (hasBody == hasTemplate || (body != null && !hasBody) || (contentSid != null && !hasTemplate)) {
            throw new IllegalArgumentException("Message requires either a non-empty body or contentSid");
        }
        if (contentVariables != null) {
            if (!hasTemplate) {
                throw new IllegalArgumentException("Content variables require a contentSid");
            }
            if (contentVariables.entrySet().stream().anyMatch(e -> e.getKey() == null || e.getKey().isBlank() || e.getValue() == null)) {
                throw new IllegalArgumentException("Content variables require non-empty keys and non-null values");
            }
            contentVariables = Map.copyOf(contentVariables);
        }
    }
}
