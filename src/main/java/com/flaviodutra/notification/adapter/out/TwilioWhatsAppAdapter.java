package com.flaviodutra.notification.adapter.out;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flaviodutra.notification.application.port.out.WhatsAppProviderPort;
import com.flaviodutra.notification.domain.model.MessageToSend;
import com.twilio.exception.ApiException;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class TwilioWhatsAppAdapter implements WhatsAppProviderPort {
    private final String messagingServiceSid;
    private final TwilioRestClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public TwilioWhatsAppAdapter(String accountSid, String authToken, String messagingServiceSid) {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()
                || messagingServiceSid == null || messagingServiceSid.isBlank()) {
            throw new IllegalArgumentException("Twilio secret must contain accountSid, authToken and fromNumber");
        }
        this.messagingServiceSid = messagingServiceSid;
        this.client = new TwilioRestClient.Builder(accountSid, authToken).build();
    }

    public TwilioWhatsAppAdapter(String messagingServiceSid, TwilioRestClient client) {
        this.messagingServiceSid = messagingServiceSid;
        this.client = client;
    }

    @Override
    public void send(MessageToSend message) {
        var creator = Message.creator(new PhoneNumber(message.to()), messagingServiceSid, message.body());
        if (message.contentSid() != null) {
            creator.setContentSid(message.contentSid());
            if (message.contentVariables() != null && !message.contentVariables().isEmpty()) {
                try {
                    creator.setContentVariables(mapper.writeValueAsString(message.contentVariables()));
                } catch (JsonProcessingException e) {
                    throw new IllegalArgumentException("Unable to serialize content variables", e);
                }
            }
        }
        try {
            Message sent = creator.create(client);
            System.out.println("Twilio accepted message " + sent.getSid() + " with initial status " + sent.getStatus());
        } catch (ApiException e) {
            System.err.println("Twilio request failed: code=" + e.getCode() + ", HTTP status=" + e.getStatusCode());
            throw e;
        }
    }
}
