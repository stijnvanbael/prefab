# Prefab Task Recipes

**Part of the [Prefab Developer Guide](../developer-guide.md).** Load this index, then open only the recipe for the
current intent. The [agent cheat sheet](../agent-cheat-sheet.md) is the always-loaded overview; the
[annotation reference](../annotation-reference.md) remains the detailed fallback.

Each recipe has a **750-token budget** (enforced as a 3,000-character Markdown limit). Complete Java examples are
linked to their canonical showcase or processor-test source instead of copied into prose. Those sources are compiled
or exercised by the Maven build, so the recipe cannot silently drift from the tested API.

| Intent | Recipe |
|---|---|
| Add a REST CRUD aggregate | [Aggregate with REST CRUD](aggregate-rest-crud.md) |
| Add a filtered, paged, sorted list | [Filtered, paged and sorted lists](filtered-paged-sorted-lists.md) |
| Implement updates and domain logic | [Updates and domain logic](updates-and-domain-logic.md) |
| Publish domain events | [Publishing events](publishing-events.md) |
| Handle events by creating an aggregate | [Event handling: create](event-handling-create.md) |
| Handle an event by updating one aggregate by reference | [Event handling: update by reference](event-handling-update-by-reference.md) |
| Handle an event across matching aggregates | [Event handling: multicast](event-handling-multicast.md) |
| Add repository queries | [Repository mixins](repository-mixins.md) |
| Secure generated endpoints | [Endpoint security](endpoint-security.md) |
| Scope data to a tenant | [Multi-tenancy](multi-tenancy.md) |
| Upload or download binary data | [Binary fields](binary-fields.md) |
| Track who created or changed an aggregate | [Audit fields](audit-fields.md) |
| Test generated endpoints and behavior | [Testing with generated helpers](testing-with-generated-helpers.md) |
