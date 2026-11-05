package com.flaviodutra.notification;

import com.flaviodutra.notification.adapter.out.SecretsManagerTwilioConfig;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import static org.junit.jupiter.api.Assertions.*;

class SecretsManagerTwilioConfigTest {
    @Test
    void readsNamedSecretAndDoesNotExposeCredentialsInToString() {
        var request = new AtomicReference<GetSecretValueRequest>();
        var config = loader("{\"accountSid\":\"AC123\",\"authToken\":\"sensitive\",\"fromNumber\":\"whatsapp:+123\"}", request).load("twilio/whatsapp");
        assertEquals("twilio/whatsapp", request.get().secretId());
        assertEquals("AC123", config.accountSid());
        assertEquals("sensitive", config.authToken());
        assertEquals("whatsapp:+123", config.fromNumber());
        assertFalse(config.toString().contains("sensitive"));
    }

    @Test
    void rejectsMissingFieldsAndMalformedSecretsWithoutEchoingContents() {
        for (String value : new String[]{"{}", "[]", "{sensitive", "{\"authToken\":\"sensitive\"}"}) {
            var exception = assertThrows(IllegalArgumentException.class,
                    () -> loader(value, new AtomicReference<>()).load("twilio/whatsapp"));
            assertFalse(exception.toString().contains("sensitive"));
        }
        assertThrows(IllegalArgumentException.class,
                () -> loader(null, new AtomicReference<>()).load("twilio/whatsapp"));
    }

    private SecretsManagerTwilioConfig loader(String value, AtomicReference<GetSecretValueRequest> request) {
        SecretsManagerClient client = (SecretsManagerClient) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{SecretsManagerClient.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getSecretValue")) {
                        request.set((GetSecretValueRequest) args[0]);
                        return GetSecretValueResponse.builder().secretString(value).build();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        return new SecretsManagerTwilioConfig(client);
    }
}
