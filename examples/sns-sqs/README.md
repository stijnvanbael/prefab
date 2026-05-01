# SNS/SQS Example

This example demonstrates how to use Prefab with AWS SNS/SQS.

## Running locally

To run the example against a local [LocalStack](https://localstack.cloud/) instance, activate the `local` Spring profile.
This profile provides `access-key: test` and `secret-key: test` without requiring environment variables to be set.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

When deploying to a real AWS environment, export the required environment variables instead:

```bash
export AWS_ACCESS_KEY_ID=<your-access-key>
export AWS_SECRET_ACCESS_KEY=<your-secret-key>
./mvnw spring-boot:run
```
