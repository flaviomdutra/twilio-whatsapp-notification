package com.flaviodutra.notification.adapter.in.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flaviodutra.notification.application.port.in.ProcessMessageUseCase;
import com.flaviodutra.notification.config.ApplicationFactory;
import com.flaviodutra.notification.domain.model.MessageToSend;
import java.util.ArrayList;

public class SqsLambdaHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {
    private final ProcessMessageUseCase useCase;
    private final ObjectMapper mapper = new ObjectMapper();

    public SqsLambdaHandler() { this(ApplicationFactory.create()); }
    public SqsLambdaHandler(ProcessMessageUseCase useCase) { this.useCase = useCase; }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        var failures = new ArrayList<SQSBatchResponse.BatchItemFailure>();
        for (var record : event.getRecords()) {
            try {
                useCase.process(mapper.readValue(record.getBody(), MessageToSend.class));
            } catch (Exception e) {
                context.getLogger().log("Failed SQS message " + record.getMessageId() + " (" + e.getClass().getSimpleName() + ")");
                failures.add(new SQSBatchResponse.BatchItemFailure(record.getMessageId()));
            }
        }
        return new SQSBatchResponse(failures);
    }
}
