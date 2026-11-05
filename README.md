# WhatsApp Notifications with SQS

The application receives JSON messages from an SQS queue, runs the use case, and delivers them through the Twilio adapter. Locally, LocalStack simulates SQS and a console adapter simulates delivery without calling Twilio.

## Configuration

Default values are in `src/main/resources/application.yaml` and are loaded at startup, including in Lambda. Set `APP_PROFILE=local` to load `application-local.yaml` instead; the local profile uses LocalStack and simulates delivery. Unknown profiles cause a startup error. Environment variables override file values, including when a variable is empty:

| Property | Environment variable |
| --- | --- |
| `sqs.queue.name` | `SQS_QUEUE_NAME` |
| `sqs.queue.url` | `SQS_QUEUE_URL` |
| `sqs.endpoint` | `SQS_ENDPOINT` |
| `whatsapp.provider` | `WHATSAPP_PROVIDER` |
| `twilio.secret.id` | `TWILIO_SECRET_ID` |
| `secrets.manager.endpoint` | `SECRETS_MANAGER_ENDPOINT` |

Configuration files are packaged in the JAR; changing them requires rebuilding it. The AWS SDK resolves the region and credentials directly, for example through `AWS_REGION` and an AWS profile or role. Never put Twilio credentials in these files; store them in Secrets Manager.

## Run locally

Requirements: Java 17+, Maven, and Docker Compose. Only LocalStack runs in Docker; the Java application runs on your machine.

```bash
docker compose up -d
mvn clean package
APP_PROFILE=local AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_REGION=us-east-1 \
  java -jar target/twilio-whatsapp-notification-1.0-SNAPSHOT.jar
```

In an IDE, run `com.flaviodutra.notification.Main` with `APP_PROFILE=local`, `AWS_ACCESS_KEY_ID=test`, `AWS_SECRET_ACCESS_KEY=test`, and `AWS_REGION=us-east-1` as environment variables. The AWS SDK requires a region and dummy credentials for LocalStack.

In another terminal, send a message:

```bash
docker compose exec localstack awslocal sqs send-message \
  --queue-url http://localhost:4566/000000000000/whatsapp-notifications \
  --message-body '{"to":"whatsapp:+5511999999999","body":"Hello!"}'
```

The application logs simulated delivery. The queue and dead-letter queue (DLQ) are created at startup. Invalid messages or delivery failures return to the queue after 60 seconds and move to `whatsapp-notifications-dlq` after three receives. Inspect the DLQ with:

```bash
docker compose exec localstack awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/whatsapp-notifications-dlq
```

Stop the consumer with Ctrl+C and the local environment with `docker compose down`.

## Send a template

For template messages, send a `contentSid` and, optionally, a `contentVariables` object instead of `body`:

```bash
docker compose exec localstack awslocal sqs send-message \
  --queue-url http://localhost:4566/000000000000/whatsapp-notifications \
  --message-body '{"to":"whatsapp:+5511999999999","contentSid":"HX_YOUR_TEMPLATE_SID","contentVariables":{"1":"Jane","2":"12345"}}'
```

Replace the recipient, Content SID, and variable names and values with those for your template. Content variables are an object of string keys and string values; omit `contentVariables` if the template needs none. Each message must contain exactly one of a non-empty `body` or `contentSid`. Invalid messages are retried and eventually sent to the DLQ.

To actually send, select `WHATSAPP_PROVIDER=twilio` and configure a Messaging Service with a WhatsApp sender. Outside the 24-hour customer service window, the template must be approved for WhatsApp. The Twilio API accepting the message does not guarantee delivery; check its final status in Twilio Messaging Logs. See [Twilio's template sending guide](https://www.twilio.com/docs/content/send-templates-created-with-the-content-template-builder).

## Configure the Twilio secret

LocalStack creates `twilio/whatsapp` with placeholder values. To load it, set the endpoints and dummy AWS credentials before starting the consumer:

```bash
export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_REGION=us-east-1
export TWILIO_SECRET_ID=twilio/whatsapp SECRETS_MANAGER_ENDPOINT=http://localhost:4566
export SQS_ENDPOINT=http://localhost:4566
WHATSAPP_PROVIDER=twilio java -jar target/twilio-whatsapp-notification-1.0-SNAPSHOT.jar
```

The `twilio` mode attempts real delivery: replace the placeholders in LocalStack only if you intend to use your account. Real messages may incur charges. For simulated delivery without calling Twilio, use `WHATSAPP_PROVIDER=console`, which does not read the secret.

In AWS Secrets Manager, create a text secret using this format (the values are placeholders):

```json
{"accountSid":"AC...","authToken":"...","fromNumber":"MG_YOUR_MESSAGING_SERVICE_SID"}
```

Set `TWILIO_SECRET_ID` to the secret name or ARN and `AWS_REGION` to its region. In AWS, leave `SECRETS_MANAGER_ENDPOINT` unset: the SDK uses its default credentials chain, including the Lambda execution role. The role needs `secretsmanager:GetSecretValue` for the secret and, if the secret uses a customer-managed KMS key, `kms:Decrypt` for that key. The `fromNumber` field holds the Messaging Service SID (`MG...`); the legacy `from` field in the message JSON is ignored.

The secret is loaded when the application or Lambda environment starts and remains in memory for its lifetime. After rotation, restart the consumer or replace the Lambda environments to load the new value. Never log or commit real credentials.

## Consume an AWS queue

The same consumer can run outside Lambda against a real SQS queue. Configure the region and a queue URL or name; without `SQS_ENDPOINT`, the SDK uses the AWS endpoint. The URL takes precedence over the name and can identify a queue in another account when permissions allow it.

```bash
export AWS_REGION=us-east-1
export SQS_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789012/whatsapp-notifications
export TWILIO_SECRET_ID=twilio/whatsapp
unset SQS_ENDPOINT SECRETS_MANAGER_ENDPOINT
WHATSAPP_PROVIDER=twilio java -jar target/twilio-whatsapp-notification-1.0-SNAPSHOT.jar
```

Alternatively, use `SQS_QUEUE_NAME` to look up the URL in the current credentials' account; the default is `whatsapp-notifications`. The SDK uses its default credentials chain, such as a local profile or instance role. Grant `sqs:ReceiveMessage` and `sqs:DeleteMessage` on the queue and, when looking it up by name, `sqs:GetQueueUrl`. For queues encrypted with a customer-managed KMS key, also grant the required KMS permissions. Provision the AWS queue and DLQ separately.

For LocalStack, set `SQS_ENDPOINT=http://localhost:4566`, a region, and dummy credentials as shown in the local example. If using `SQS_QUEUE_URL`, ensure it points to the intended environment. `WHATSAPP_PROVIDER=console` simulates WhatsApp delivery only; it does not prevent consumption or deletion of messages from a real AWS queue.

## Architecture and delivery

The domain and input/output ports are independent of SQS and Twilio. The input adapters (SQS polling and Lambda) convert JSON and call the same use case; the output adapter implements the WhatsApp port. `ApplicationFactory` wires the components together.

The Lambda handler is `com.flaviodutra.notification.adapter.in.lambda.SqsLambdaHandler::handleRequest`. Enable `ReportBatchItemFailures` in the Lambda SQS event source mapping so only failed records are retried. This project does not provision Lambda infrastructure. SQS provides at-least-once delivery: failures after sending but before acknowledgment can cause duplicate messages. Persistent idempotency is needed if duplicates are unacceptable.
