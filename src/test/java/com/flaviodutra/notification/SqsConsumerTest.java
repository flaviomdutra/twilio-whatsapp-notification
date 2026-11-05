package com.flaviodutra.notification;

import com.flaviodutra.notification.adapter.in.sqs.SqsConsumer;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import static org.junit.jupiter.api.Assertions.*;

class SqsConsumerTest {
    @Test
    void deletesOnlySuccessfullyProcessedMessagesAndContinuesAfterFailures() {
        var deleted = new ArrayList<DeleteMessageRequest>();
        var processed = new ArrayList<String>();
        var received = new ArrayList<ReceiveMessageRequest>();
        var messages = List.of(
                message("one", "receipt-one", "{\"to\":\"whatsapp:+1\",\"body\":\"ok\"}"),
                message("bad", "receipt-bad", "{\"body\":\"missing recipient\"}"),
                message("failure", "receipt-failure", "{\"to\":\"whatsapp:+2\",\"body\":\"fail\"}"),
                message("two", "receipt-two", "{\"to\":\"whatsapp:+3\",\"body\":\"ok\"}"));
        SqsClient client = (SqsClient) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{SqsClient.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("receiveMessage")) {
                        received.add((ReceiveMessageRequest) args[0]);
                        return ReceiveMessageResponse.builder().messages(messages).build();
                    }
                    if (method.getName().equals("deleteMessage")) {
                        deleted.add((DeleteMessageRequest) args[0]);
                        return DeleteMessageResponse.builder().build();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        new SqsConsumer(client, "queue-url", message -> {
            if (message.body().equals("fail")) throw new IllegalStateException();
            processed.add(message.to());
        }).poll();
        assertEquals(List.of("whatsapp:+1", "whatsapp:+3"), processed);
        assertEquals(List.of("receipt-one", "receipt-two"), deleted.stream().map(DeleteMessageRequest::receiptHandle).toList());
        assertTrue(deleted.stream().allMatch(request -> request.queueUrl().equals("queue-url")));
        assertEquals(20, received.get(0).waitTimeSeconds());
        assertEquals("queue-url", received.get(0).queueUrl());
    }

    private Message message(String id, String receipt, String body) {
        return Message.builder().messageId(id).receiptHandle(receipt).body(body).build();
    }
}
