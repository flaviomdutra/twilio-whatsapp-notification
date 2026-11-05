package com.flaviodutra.notification.config;

import java.net.URI;

import com.flaviodutra.notification.adapter.out.SecretsManagerTwilioConfig;
import com.flaviodutra.notification.adapter.out.TwilioWhatsAppAdapter;
import com.flaviodutra.notification.application.port.in.ProcessMessageUseCase;
import com.flaviodutra.notification.application.port.out.WhatsAppProviderPort;
import com.flaviodutra.notification.domain.service.SendMessageService;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public final class ApplicationFactory {
    private ApplicationFactory() {
    }

    public static ProcessMessageUseCase create() {
        return create(ApplicationSettings.load());
    }

    public static ProcessMessageUseCase create(ApplicationSettings settings) {
        String provider = settings.get("whatsapp.provider", "WHATSAPP_PROVIDER");
        WhatsAppProviderPort adapter;
        if ("console".equalsIgnoreCase(provider)) {
            adapter = message -> System.out.println("Simulated WhatsApp delivery to " + message.to());
        } else if ("twilio".equalsIgnoreCase(provider)) {
            String secretId = settings.get("twilio.secret.id", "TWILIO_SECRET_ID");
            if (secretId == null || secretId.isBlank()) {
                throw new IllegalArgumentException("TWILIO_SECRET_ID is required when WHATSAPP_PROVIDER=twilio");
            }
            var builder = SecretsManagerClient.builder();
            String endpoint = settings.get("secrets.manager.endpoint", "SECRETS_MANAGER_ENDPOINT");
            if (endpoint != null && !endpoint.isBlank()) {
                builder.endpointOverride(URI.create(endpoint));
            }
            try (var client = builder.build()) {
                var config = new SecretsManagerTwilioConfig(client).load(secretId);
                adapter = new TwilioWhatsAppAdapter(config.accountSid(), config.authToken(), config.fromNumber());
            }
        } else {
            throw new IllegalArgumentException("Unknown WHATSAPP_PROVIDER: " + provider);
        }
        return new SendMessageService(adapter);
    }
}
