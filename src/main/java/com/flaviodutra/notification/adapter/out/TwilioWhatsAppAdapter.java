package com.flaviodutra.notification.adapter.out;

import com.flaviodutra.notification.application.port.out.WhatsAppProviderPort;
import com.flaviodutra.notification.domain.model.MessageToSend;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

/**
 * TwilioWhatsAppAdapter
 */
public class TwilioWhatsAppAdapter implements WhatsAppProviderPort {
  private final String accountSid;
  private final String authToken;
  private final String fromNumber;

  public TwilioWhatsAppAdapter(String accountSid, String authToken, String fromNumber) {
    this.accountSid = accountSid;
    this.authToken = authToken;
    this.fromNumber = fromNumber;
    Twilio.init(accountSid, authToken);
  }

  @Override
  public void send(MessageToSend message) {
    Message.creator(
        new PhoneNumber(message.to()),
        message.from(),
        message.body())
        .create();
  }
}
