package com.flaviodutra.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.flaviodutra.notification.config.ApplicationSettings;

class ApplicationSettingsTest {
    @Test
    void readsNestedYamlAndEnvironmentOverridesIncludingAnEmptyValue() throws Exception {
        String yaml = "sqs:\n  endpoint: http://localhost:4566\n  queue:\n    name: test-queue\n";
        var defaults = settings(yaml, Map.of());
        assertEquals("http://localhost:4566", defaults.get("sqs.endpoint", "SQS_ENDPOINT"));
        assertEquals("test-queue", defaults.get("sqs.queue.name", "SQS_QUEUE_NAME"));
        assertEquals("", defaults.get("sqs.queue.url", "SQS_QUEUE_URL"));
        assertEquals("", settings(yaml, Map.of("SQS_ENDPOINT", "")).get("sqs.endpoint", "SQS_ENDPOINT"));
        assertEquals("other",
                settings(yaml, Map.of("SQS_QUEUE_NAME", "other")).get("sqs.queue.name", "SQS_QUEUE_NAME"));
    }

    @Test
    void loadsBundledDefaults() {
        assertEquals("whatsapp-notifications",
                ApplicationSettings.load().get("sqs.queue.name", "UNUSED_TEST_VARIABLE"));
        assertEquals("twilio", ApplicationSettings.load().get("whatsapp.provider", "UNUSED_TEST_VARIABLE"));
    }

    @Test
    void selectsLocalProfileAndAllowsEnvironmentOverrides() {
        var local = ApplicationSettings.load(Map.of("APP_PROFILE", "local"));
        assertEquals("http://localhost:4566", local.get("sqs.endpoint", "SQS_ENDPOINT"));
        assertEquals("console", local.get("whatsapp.provider", "WHATSAPP_PROVIDER"));
        assertEquals("twilio/whatsapp", local.get("twilio.secret.id", "TWILIO_SECRET_ID"));
        assertEquals("other", ApplicationSettings.load(Map.of("APP_PROFILE", "local", "SQS_QUEUE_NAME", "other"))
                .get("sqs.queue.name", "SQS_QUEUE_NAME"));
        var standard = ApplicationSettings.load(Map.of());
        assertEquals("", standard.get("sqs.endpoint", "SQS_ENDPOINT"));
        assertEquals("twilio", standard.get("whatsapp.provider", "WHATSAPP_PROVIDER"));
        assertThrows(IllegalArgumentException.class, () -> ApplicationSettings.load(Map.of("APP_PROFILE", "unknown")));
    }

    @Test
    void rejectsInvalidConfigurationStructure() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> settings("- unexpected", Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> settings("sqs:\n  endpoint: 123", Map.of()).get("sqs.endpoint", "SQS_ENDPOINT"));
    }

    private ApplicationSettings settings(String yaml, Map<String, String> environment) throws Exception {
        return ApplicationSettings.fromYaml(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
                environment);
    }
}
