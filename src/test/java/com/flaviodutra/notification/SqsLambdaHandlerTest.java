package com.flaviodutra.notification;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.flaviodutra.notification.adapter.in.lambda.SqsLambdaHandler;
import com.flaviodutra.notification.domain.model.MessageToSend;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SqsLambdaHandlerTest {
    @Test
    void reportsOnlyFailedRecordsAndProcessesRemainingMessages() {
        var processed = new ArrayList<MessageToSend>();
        var handler = new SqsLambdaHandler(processed::add);
        var event = new SQSEvent();
        event.setRecords(List.of(record("ok", "{\"to\":\"whatsapp:+5511\",\"body\":\"hello\"}"),
                record("bad", "{\"body\":\"missing destination\"}"),
                record("ok2", "{\"to\":\"whatsapp:+5522\",\"body\":\"hello\"}")));
        LambdaLogger logger = new LambdaLogger() {
            public void log(String message) {}
            public void log(byte[] message) {}
        };
        Context context = (Context) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Context.class},
                (proxy, method, args) -> method.getName().equals("getLogger") ? logger : null);
        var result = handler.handleRequest(event, context);
        assertEquals(2, processed.size());
        assertEquals(List.of("bad"), result.getBatchItemFailures().stream().map(f -> f.getItemIdentifier()).toList());
    }

    private SQSEvent.SQSMessage record(String id, String body) {
        var message = new SQSEvent.SQSMessage();
        message.setMessageId(id);
        message.setBody(body);
        return message;
    }
}
