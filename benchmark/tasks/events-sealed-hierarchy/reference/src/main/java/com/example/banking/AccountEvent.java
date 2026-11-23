package com.example.banking;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@Event(topic = "account-events")
public sealed interface AccountEvent permits AccountEvent.Deposited, AccountEvent.Withdrawn {
    @PartitioningKey
    Reference<Account> account();

    record Deposited(Reference<Account> account, BigDecimal amount) implements AccountEvent {
    }

    record Withdrawn(Reference<Account> account, BigDecimal amount) implements AccountEvent {
    }
}
