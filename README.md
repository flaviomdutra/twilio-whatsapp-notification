# Notificações WhatsApp com SQS

A aplicação recebe mensagens JSON de uma fila SQS, executa o caso de uso e entrega pelo adaptador Twilio. Localmente, o LocalStack simula o SQS e um adaptador de console simula a entrega, sem chamar a Twilio.

## Configuração

Os valores padrão ficam em `src/main/resources/application.yaml` e são carregados na inicialização, inclusive pela Lambda. Variáveis de ambiente têm prioridade sobre o arquivo; uma variável vazia também substitui o padrão. Os mapeamentos são:

| Propriedade | Variável de ambiente |
| --- | --- |
| `sqs.queue.name` | `SQS_QUEUE_NAME` |
| `sqs.queue.url` | `SQS_QUEUE_URL` |
| `sqs.endpoint` | `SQS_ENDPOINT` |
| `whatsapp.provider` | `WHATSAPP_PROVIDER` |
| `twilio.secret.id` | `TWILIO_SECRET_ID` |
| `secrets.manager.endpoint` | `SECRETS_MANAGER_ENDPOINT` |

As configurações do arquivo são empacotadas no JAR; alterá-las requer reconstruí-lo. Região e credenciais AWS são resolvidas diretamente pelo SDK, por exemplo via `AWS_REGION` e perfil ou role AWS. Nunca coloque credenciais Twilio no arquivo: armazene-as no Secrets Manager.

## Executar localmente

Requisitos: Java 17+, Maven e Docker com Compose.

```bash
docker compose up -d
mvn clean package
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_REGION=us-east-1 \
  SQS_ENDPOINT=http://localhost:4566 WHATSAPP_PROVIDER=console \
  java -jar target/twilio-whatsapp-notification-1.0-SNAPSHOT.jar
```

Em outro terminal, envie uma mensagem:

```bash
docker compose exec localstack awslocal sqs send-message \
  --queue-url http://localhost:4566/000000000000/whatsapp-notifications \
  --message-body '{"to":"whatsapp:+5511999999999","body":"Olá!"}'
```

O processo exibe a entrega simulada. A fila e a DLQ são criadas na inicialização. Mensagens inválidas ou falhas de entrega voltam à fila após 60 segundos e seguem para `whatsapp-notifications-dlq` após três recebimentos. Consulte-a com `docker compose exec localstack awslocal sqs receive-message --queue-url http://localhost:4566/000000000000/whatsapp-notifications-dlq`. Encerre o consumidor com Ctrl+C e o ambiente com `docker compose down`.

## Configurar o segredo da Twilio

O LocalStack cria `twilio/whatsapp` com valores fictícios. Para testá-lo, defina o endpoint e as credenciais fictícias da AWS antes de executar o consumidor:

```bash
export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_REGION=us-east-1
export TWILIO_SECRET_ID=twilio/whatsapp SECRETS_MANAGER_ENDPOINT=http://localhost:4566
export SQS_ENDPOINT=http://localhost:4566
WHATSAPP_PROVIDER=twilio java -jar target/twilio-whatsapp-notification-1.0-SNAPSHOT.jar
```

O modo `twilio` tenta enviar mensagens reais: substitua os valores fictícios no Secrets Manager local apenas se desejar usar sua conta, e saiba que envios reais podem gerar custos. Para simular a entrega sem acessar a Twilio, continue usando `WHATSAPP_PROVIDER=console`, que não lê o segredo.

No AWS Secrets Manager, crie um segredo do tipo texto com este formato (os valores abaixo são exemplos fictícios):

```json
{"accountSid":"AC...","authToken":"...","fromNumber":"whatsapp:+14155238886"}
```

Configure `TWILIO_SECRET_ID` com o nome ou ARN do segredo e `AWS_REGION` com a região. Em AWS, deixe `SECRETS_MANAGER_ENDPOINT` indefinido: o SDK usa a cadeia padrão de credenciais, incluindo a role de execução da Lambda. A role precisa de `secretsmanager:GetSecretValue` para esse segredo e, se ele usar chave KMS gerenciada pelo cliente, `kms:Decrypt` nessa chave. O remetente vem do segredo; o campo legado `from` no JSON da mensagem é ignorado.

O segredo é carregado na inicialização da aplicação ou do ambiente Lambda e permanece em memória durante sua vida útil. Após rotacioná-lo, reinicie o consumidor ou substitua os ambientes Lambda para carregar o novo valor. Nunca registre ou versione credenciais reais.

## Consumir uma fila na AWS

O mesmo consumidor pode executar fora da Lambda com uma fila SQS real. Configure a região e uma URL ou nome de fila; sem `SQS_ENDPOINT`, o SDK usa o endpoint da AWS. A URL tem prioridade sobre o nome e permite especificar diretamente uma fila de outra conta, se as permissões permitirem.

```bash
export AWS_REGION=us-east-1
export SQS_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789012/whatsapp-notifications
export TWILIO_SECRET_ID=twilio/whatsapp
unset SQS_ENDPOINT SECRETS_MANAGER_ENDPOINT
WHATSAPP_PROVIDER=twilio java -jar target/twilio-whatsapp-notification-1.0-SNAPSHOT.jar
```

Opcionalmente, use `SQS_QUEUE_NAME` para buscar a URL na conta das credenciais atuais; o padrão é `whatsapp-notifications`. O SDK usa sua cadeia padrão de credenciais (por exemplo, perfil local ou role da instância). Conceda `sqs:ReceiveMessage` e `sqs:DeleteMessage` na fila e, quando usar o nome, `sqs:GetQueueUrl`. Para filas criptografadas com chave KMS gerenciada pelo cliente, conceda também as permissões KMS necessárias. A fila e a DLQ na AWS devem ser provisionadas separadamente.

Para LocalStack, configure `SQS_ENDPOINT=http://localhost:4566`, região e credenciais fictícias conforme o exemplo local. Se usar `SQS_QUEUE_URL`, ela deve apontar para o ambiente pretendido. A opção `WHATSAPP_PROVIDER=console` simula apenas a entrega pelo WhatsApp; ela não impede o consumo ou a exclusão de mensagens de uma fila AWS real.

## Arquitetura e entrega

O domínio e as portas de entrada/saída são independentes do SQS e da Twilio. Os adaptadores de entrada (polling SQS e Lambda) convertem JSON e chamam o mesmo caso de uso; o adaptador de saída implementa a porta de WhatsApp. A composição fica em `ApplicationFactory`.

O handler Lambda é `com.flaviodutra.notification.adapter.in.lambda.SqsLambdaHandler::handleRequest`. Ao configurar o mapeamento SQS da Lambda, habilite `ReportBatchItemFailures` para que somente os registros com falha sejam reprocessados. A infraestrutura da Lambda não é criada por este projeto. O SQS oferece entrega pelo menos uma vez: falhas após o envio e antes da confirmação podem causar mensagens duplicadas; idempotência persistente é necessária se duplicações forem inaceitáveis.
