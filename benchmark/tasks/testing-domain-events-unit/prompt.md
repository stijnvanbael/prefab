The `com.example.banking.Account` aggregate publishes an `AccountEvent.Deposited` event on every deposit and an
`AccountEvent.Withdrawn` event on every withdrawal, and refuses withdrawals that exceed the balance (it throws
`be.appify.prefab.core.problem.ConflictException`).

Write fast unit tests for this behaviour: plain JUnit tests that call the aggregate directly, without starting a
Spring context and without Docker. Verify which events are published, with which amounts, and that an overdraft is
refused without publishing anything. Prefab offers test support for capturing published domain events in unit tests;
use it.

Do not change the production code; only add tests under `src/test/java`. All tests must pass.
