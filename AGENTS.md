# Repository Guidelines

## Project Structure & Architecture

This is a Java 17 Maven application for WhatsApp notifications. Production code lives under `src/main/java/com/flaviodutra/notification`:

- `domain/` contains the message model and business service; `application/port/` defines use cases and provider interfaces.
- `adapter/in/` contains the Lambda and SQS consumers; `adapter/out/` contains the Twilio adapter. Wire implementations in `config/ApplicationFactory.java`.
- `src/test/java/` contains JUnit tests, `src/main/resources/application.yaml` holds non-secret defaults (environment variables override them), and `docker/localstack/init.sh` creates the local queues and DLQ.

Keep domain and application ports independent of AWS, Twilio, and transport details. Add integrations behind adapters and inject dependencies into the use case.

## Build, Test, and Development Commands

Prerequisites: Java 17+, Maven, and Docker Compose.

- `mvn test` runs the unit tests.
- `mvn clean package` builds and tests the application and creates the executable JAR in `target/`.
- `docker compose up -d` starts LocalStack and initializes the SQS queues.
- `AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_REGION=us-east-1 SQS_ENDPOINT=http://localhost:4566 WHATSAPP_PROVIDER=console java -jar target/twilio-whatsapp-notification-1.0-SNAPSHOT.jar` starts the LocalStack consumer with simulated delivery.
- `docker compose down` stops the local environment. See `README.md` for an example SQS message and DLQ inspection.

## Coding Style & Naming

Follow the surrounding Java style and use consistent indentation within each file; new Java code should use four spaces. Use PascalCase for classes, camelCase for methods and fields, and lowercase package names. Name ports by their role, such as `WhatsAppProviderPort`, and adapters by their integration. No formatter or linter is currently configured.

## Testing Guidelines

Use JUnit Jupiter and name test classes `*Test` under `src/test/java/`. Test behavior through ports or injected collaborators, including successful processing and failure handling. Run `mvn test` before submitting changes; use LocalStack for changes to queue initialization or the consumer. No coverage threshold is configured.

## Commits & Pull Requests

Existing commit subjects use short, imperative English descriptions, for example `Add AWS Lambda dependencies and improve error handling in SqsLambdaHandler`. Follow that pattern and keep each commit focused. Pull requests should explain the behavior change, list validation performed, and link relevant issues when available. Document configuration or queue changes in the README.

## Security & Configuration

Do not commit credentials or `.env` files. Configure Twilio through AWS Secrets Manager using `TWILIO_SECRET_ID`; prefer `WHATSAPP_PROVIDER=console` for local tests. SQS delivers at least once, so preserve retry behavior and consider duplicate deliveries when changing processing logic.
