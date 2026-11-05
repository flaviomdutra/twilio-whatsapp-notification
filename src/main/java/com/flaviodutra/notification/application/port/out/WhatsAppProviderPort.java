package com.flaviodutra.notification.application.port.out;

import com.flaviodutra.notification.domain.model.MessageToSend;

/**
 * WhatsAppProviderPort
 */
public interface WhatsAppProviderPort {
  void send(MessageToSend message);
}
