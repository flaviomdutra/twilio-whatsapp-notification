package com.flaviodutra.notification.domain.model;

/**
 * MessageToSend
 */
public record MessageToSend(String to, String from, String body) {
}
