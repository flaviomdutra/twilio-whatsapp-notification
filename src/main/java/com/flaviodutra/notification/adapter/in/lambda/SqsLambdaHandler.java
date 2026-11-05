package com.flaviodutra.notification.adapter.in.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flaviodutra.notification.adapter.out.TwilioWhatsAppAdapter;
import com.flaviodutra.notification.application.port.in.ProcessMessageUseCase;
import com.flaviodutra.notification.application.port.out.WhatsAppProviderPort;
import com.flaviodutra.notification.domain.service.SendMessageService;
import com.flaviodutra.notification.domain.model.MessageToSend;
public class SqsLambdaHandler implements RequestHandler<SQSEvent, Void> {

    private final ProcessMessageUseCase useCase;
    private final ObjectMapper mapper = new ObjectMapper();

    public SqsLambdaHandler() {
        String accountSid = System.getenv("TWILIO_ACCOUNT_SID");
        String authToken  = System.getenv("TWILIO_AUTH_TOKEN");
        String fromNumber = System.getenv("TWILIO_FROM_NUMBER");

        WhatsAppProviderPort whatsAppProvider = new TwilioWhatsAppAdapter(accountSid, authToken, fromNumber);
        this.useCase = new SendMessageService(whatsAppProvider);
    }

 @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        for (SQSMessage msg : sqsEvent.getRecords()) {
            processMessage(msg, context);
        }
        context.getLogger().log("done");
        return null;
    }

    private void processMessage(SQSMessage msg, Context context) {
        try {
            MessageToSend message = mapper.readValue(msg.getBody(), MessageToSend.class);
            useCase.process(message);
            context.getLogger().log("Processed SQS message " + msg.getMessageId());
        } catch (Exception e) {
            context.getLogger().log("Failed to process SQS message " + msg.getMessageId() + ": " + e.getMessage());
            throw e;
        }

    }
}
