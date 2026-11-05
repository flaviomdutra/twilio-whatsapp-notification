package com.flaviodutra.notification;

import java.net.URI;

import com.flaviodutra.notification.adapter.in.sqs.SqsConsumer;
import com.flaviodutra.notification.config.ApplicationFactory;
import com.flaviodutra.notification.config.ApplicationSettings;

import software.amazon.awssdk.services.sqs.SqsClient;

public class Main {
    public static void main(String[] args) {
        var settings = ApplicationSettings.load();
        String queueName = settings.get("sqs.queue.name", "SQS_QUEUE_NAME");
        String endpoint = settings.get("sqs.endpoint", "SQS_ENDPOINT");
        var builder = SqsClient.builder();
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        try (var sqs = builder.build()) {
            String queueUrl = settings.get("sqs.queue.url", "SQS_QUEUE_URL");
            if (queueUrl == null || queueUrl.isBlank()) {
                queueUrl = sqs.getQueueUrl(r -> r.queueName(queueName)).queueUrl();
            }
            var consumer = new SqsConsumer(sqs, queueUrl, ApplicationFactory.create(settings));
            System.out.println("Listening to SQS queue " + queueUrl);
            while (!Thread.currentThread().isInterrupted())
                consumer.poll();
        }
    }
}
