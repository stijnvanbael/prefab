Accounts (`com.example.banking.Account`) support deposits (`POST /accounts/{id}/deposits`) and withdrawals
(`POST /accounts/{id}/withdrawals`). Publish an event to the Kafka topic `account-events` for every deposit and
withdrawal, so that downstream services can follow balances:

- define a sealed interface `com.example.banking.AccountEvent` with two nested records:
  `AccountEvent.Deposited(Reference<Account> account, BigDecimal amount)` and
  `AccountEvent.Withdrawn(Reference<Account> account, BigDecimal amount)`
  (`Reference` is `be.appify.prefab.core.service.Reference`);
- consumers read the topic as `AccountEvent`, so both event types share that topic;
- all events of one account must stay in order, so partition them by account.
