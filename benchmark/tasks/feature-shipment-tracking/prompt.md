We are building parcel tracking for a web shop as a Spring Boot application with Prefab. The project is empty; put
the code in package `com.example.shipping`.

**User story.** As a customer, I want to follow my parcel from the warehouse to my door and be notified when it
arrives; as the carrier, I want to report every scan of a parcel.

Acceptance criteria:

1. `POST /shipments` with `{"orderNumber": "SO-1001", "address": "Main Street 1, Ghent"}` registers a shipment
   (`201 Created`, `Location` header ending in the id). `GET /shipments/{id}` returns `id`, `orderNumber`, `address`,
   `status` (`CREATED`), `scanCount` (0) and `lastLocation` (`null` until the first scan). Order number and address are
   required (`400 Bad Request` otherwise).
2. The carrier reports a scan with `POST /shipments/{shipmentId}/scans` and
   `{"location": "Antwerp hub", "type": "PICKED_UP"}` (`201 Created`). The type is `PICKED_UP`, `IN_TRANSIT` or
   `DELIVERED`; anything else is `400 Bad Request`. `GET /shipments/{shipmentId}/scans` lists the scans of that
   shipment as a page (in the `content` array, with `id`, `location` and `type`).
3. Every scan increases the shipment's `scanCount` and sets its `lastLocation`. The shipment's status becomes the type
   of the latest scan, except that a `DELIVERED` shipment stays `DELIVERED`.
4. When a shipment becomes `DELIVERED`, a delivery notification is created automatically, exactly once per shipment
   (also when the carrier scans the delivery twice). `GET /delivery-notifications?orderNumber=<order number>` returns
   them as a page (in the `content` array) with `id`, `orderNumber`, `address` and `shipment`.
5. `GET /shipments?status=<status>` lists the shipments with that status, as a page.

The shipment and the notifications may lag a moment behind the scans.
