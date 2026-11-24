# Showcase: Postgres + AWS SNS/SQS

Configuration-only variant of the [showcase](../showcase): it contains no Java sources, copies those of the showcase
and recompiles them against `prefab-sns-sqs` (see
[decision-007](../../backlog/decisions/decision-007%20-%20One-showcase-recompiled-per-technology-variant.md)).
It includes the streams slice on `prefab-streams-sns-sqs` with state in Postgres (`prefab-streams-postgres`). Avro and
the Kafka-only streams breakout are declared as gaps in
[`showcase-capabilities.yml`](src/test/resources/showcase-capabilities.yml).

## Running locally

To run the application against a local [LocalStack](https://localstack.cloud/) instance (port 4566) and a local Postgres,
activate the `local` Spring profile, which is the default. The profile provides `access-key: test` and
`secret-key: test` and points the AWS endpoint to LocalStack, so no environment variables are needed:

```bash
./mvnw spring-boot:run -pl examples/showcase-sns-sqs -Dspring-boot.run.profiles=local
```

The `local` profile is active by default. When deploying to a real AWS environment, activate another profile and
export the required environment variables instead:

```bash
export AWS_ACCESS_KEY_ID=<your-access-key>
export AWS_SECRET_ACCESS_KEY=<your-secret-key>
./mvnw spring-boot:run -pl examples/showcase-sns-sqs -Dspring-boot.run.profiles=aws
```
