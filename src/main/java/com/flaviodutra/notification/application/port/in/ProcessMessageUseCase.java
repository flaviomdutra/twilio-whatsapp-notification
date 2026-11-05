package com.flaviodutra.notification.application.port.in;

import com.flaviodutra.notification.domain.model.MessageToSend;

/**
 * ProcessMessageUseCase
 */
public interface ProcessMessageUseCase {

  void process(MessageToSend message);
}
