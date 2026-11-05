#!/bin/sh
set -eu
awslocal sqs create-queue --queue-name whatsapp-notifications-dlq >/dev/null
DLQ_ARN=$(awslocal sqs get-queue-attributes --queue-url "$(awslocal sqs get-queue-url --queue-name whatsapp-notifications-dlq --query QueueUrl --output text)" --attribute-names QueueArn --query Attributes.QueueArn --output text)
awslocal sqs create-queue --queue-name whatsapp-notifications --attributes "{\"VisibilityTimeout\":\"60\",\"ReceiveMessageWaitTimeSeconds\":\"20\",\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}" >/dev/null
awslocal secretsmanager create-secret --name twilio/whatsapp --secret-string '{"accountSid":"AC00000000000000000000000000000000","authToken":"local-placeholder","fromNumber":"MG00000000000000000000000000000000"}' >/dev/null
