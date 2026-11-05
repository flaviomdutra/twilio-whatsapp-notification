package com.flaviodutra.notification.adapter.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

/** Loads infrastructure configuration without exposing secret contents in errors or logs. */
public final class SecretsManagerTwilioConfig {
    private final SecretsManagerClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public SecretsManagerTwilioConfig(SecretsManagerClient client) {
        this.client = client;
    }

    public Credentials load(String secretId) {
        String value = client.getSecretValue(GetSecretValueRequest.builder().secretId(secretId).build()).secretString();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Twilio secret must contain a JSON SecretString");
        }
        try {
            JsonNode json = mapper.readTree(value);
            if (json == null || !json.isObject()) {
                throw new IllegalArgumentException("Twilio secret must be a JSON object");
            }
            return new Credentials(required(json, "accountSid"), required(json, "authToken"), required(json, "fromNumber"));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Twilio secret must be a valid JSON object");
        }
    }

    private static String required(JsonNode json, String field) {
        JsonNode value = json.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("Twilio secret requires a non-empty " + field);
        }
        return value.textValue();
    }

    public record Credentials(String accountSid, String authToken, String fromNumber) {
        @Override public String toString() { return "Credentials[redacted]"; }
    }
}
