package com.flaviodutra.notification.domain.model;

public record MessageToSend(String to, String from, String body) {
    public MessageToSend {
        if (to == null || to.isBlank() || body == null || body.isBlank()) {
            throw new IllegalArgumentException("Message requires a destination and body");
        }
    }
}
