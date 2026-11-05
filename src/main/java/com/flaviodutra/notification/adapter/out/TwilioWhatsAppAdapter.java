package com.flaviodutra.notification.adapter.out;

import com.flaviodutra.notification.application.port.out.WhatsAppProviderPort;
import com.flaviodutra.notification.domain.model.MessageToSend;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class TwilioWhatsAppAdapter implements WhatsAppProviderPort {
    private final String fromNumber;

    public TwilioWhatsAppAdapter(String accountSid, String authToken, String fromNumber) {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()
                || fromNumber == null || fromNumber.isBlank()) {
            throw new IllegalArgumentException("Twilio secret must contain accountSid, authToken and fromNumber");
        }
        this.fromNumber = fromNumber;
        Twilio.init(accountSid, authToken);
    }

    @Override
    public void send(MessageToSend message) {
        Message.creator(new PhoneNumber(message.to()), fromNumber, message.body()).create();
    }
}
