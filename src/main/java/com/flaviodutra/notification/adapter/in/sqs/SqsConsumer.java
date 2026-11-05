package com.flaviodutra.notification.adapter.in.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flaviodutra.notification.application.port.in.ProcessMessageUseCase;
import com.flaviodutra.notification.domain.model.MessageToSend;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

public final class SqsConsumer {
    private final SqsClient sqs;
    private final String queueUrl;
    private final ProcessMessageUseCase useCase;
    private final ObjectMapper mapper = new ObjectMapper();

    public SqsConsumer(SqsClient sqs, String queueUrl, ProcessMessageUseCase useCase) {
        this.sqs = sqs;
        this.queueUrl = queueUrl;
        this.useCase = useCase;
    }

    public void poll() {
        var messages = sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).waitTimeSeconds(20).maxNumberOfMessages(10).build()).messages();
        for (var message : messages) {
            try {
                useCase.process(mapper.readValue(message.body(), MessageToSend.class));
                sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());
                System.out.println("Processed SQS message " + message.messageId());
            } catch (Exception e) {
                System.err.println("Failed SQS message " + message.messageId() + " (" + e.getClass().getSimpleName() + "); it will be retried");
            }
        }
    }
}
