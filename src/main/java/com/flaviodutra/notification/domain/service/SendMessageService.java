package com.flaviodutra.notification.domain.service;

import com.flaviodutra.notification.application.port.in.ProcessMessageUseCase;
import com.flaviodutra.notification.application.port.out.WhatsAppProviderPort;
import com.flaviodutra.notification.domain.model.MessageToSend;

/**
 * SendMessageService
 */
public class SendMessageService implements ProcessMessageUseCase {

  private final WhatsAppProviderPort whatsAppProvider;

  public SendMessageService(WhatsAppProviderPort whatsAppProvider) {
    this.whatsAppProvider = whatsAppProvider;
  }

  @Override
  public void process(MessageToSend message) {
    // Validation and business rules can be applied here before sending the message
    whatsAppProvider.send(message);
  }
}
