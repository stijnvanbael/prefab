---
id: TASK-110
title: Outgoing webhook delivery (@Webhook)
status: To Do
assignee: []
created_date: '2026-04-09 15:30'
updated_date: '2026-04-24 06:57'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 131000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Modern business integrations rely on webhooks: when something interesting happens in one system (an order is placed, a payment fails, a document is signed), that system POSTs a JSON payload to a URL registered by the receiving party. Without framework support developers must hand-write HTTP clients, retry logic, signature verification, delivery tracking, and admin screens – easily weeks of effort.

Prefab should expose a `@Webhook` annotation that hooks into the existing `@Event` machinery and generates all the plumbing for reliable outgoing webhook delivery.

Example usage:

```java
@Event
@Webhook   // whenever this event is published, deliver it to all registered webhook endpoints
public record InvoicePaid(
    Reference<Invoice> invoiceId,
    BigDecimal amount,
    Instant paidAt
) { }
```

The framework maintains a `webhook_subscription` table (auto-migrated by Prefab) and exposes a management REST API for registering and removing subscriptions. When an `@Event` annotated with `@Webhook` is published, Prefab delivers the event payload as a JSON POST to each active subscription URL, with:
- Configurable retry with exponential back-off (default 3 retries, delays 1 s / 5 s / 30 s)
- HMAC-SHA256 request signature in a standard `X-Prefab-Signature` header so receivers can verify authenticity
- Delivery log with per-attempt status so operators can monitor and replay failed deliveries
- A generated management API (`POST /webhooks/subscriptions`, `GET /webhooks/subscriptions`, `DELETE /webhooks/subscriptions/{id}`) secured by a configurable role
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Add @Webhook annotation to prefab-core that can be placed on @Event-annotated classes
- [ ] #2 The annotation processor generates a WebhookDeliveryService Spring component with a method annotated with @EventListener(EventName.class) that queries all active subscriptions and delivers the payload via HTTP POST
- [ ] #3 Create a webhook_subscription table via a generated Flyway migration containing: id, event_type, url, secret (for HMAC), active flag, created_at; for MongoDB generate the corresponding collection
- [ ] #4 Generate a WebhookSubscriptionController that exposes POST /webhooks/subscriptions (create), GET /webhooks/subscriptions (list), PATCH /webhooks/subscriptions/{id}/activate|deactivate, and DELETE /webhooks/subscriptions/{id}; secured by a role configurable via prefab.webhook.management-role (default ROLE_ADMIN)
- [ ] #5 Each delivery attempt signs the JSON payload body with HMAC-SHA256 using the subscription secret and sets the X-Prefab-Signature: sha256={hex} header so receivers can verify authenticity
- [ ] #6 Delivery uses a configurable retry template: default 3 retries with exponential back-off (1 s, 5 s, 30 s); configurable via prefab.webhook.max-retries and prefab.webhook.backoff.* properties
- [ ] #7 Generate a webhook_delivery_log table (id, subscription_id, event_type, payload, attempt, status, response_code, delivered_at) and a GET /webhooks/deliveries endpoint for observability
- [ ] #8 A @Webhook annotation on a class that is not also annotated with @Event produces a clear compiler error
- [ ] #9 Add annotation-processor unit tests and an integration test for the WebhookDeliveryService using a WireMock stub as the target URL
- [ ] #10 README updated with an 'Outgoing webhooks' section describing @Webhook, the subscription API, HMAC verification, and retry configuration
<!-- AC:END -->
