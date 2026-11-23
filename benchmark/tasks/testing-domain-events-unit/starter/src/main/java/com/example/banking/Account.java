package com.example.banking;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.problem.ConflictException;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Account(
        @Id Reference<Account> id,
        @Version long version,
        @NotNull String owner,
        @NotNull BigDecimal balance
) {
    private static final EventPublisher<AccountEvent> EVENTS = EventPublisher.bind(AccountEvent.class);

    @Create
    public Account(@NotNull String owner) {
        this(Reference.create(), 0L, owner, BigDecimal.ZERO);
    }

    @Update(path = "/deposits", method = "POST")
    public Account deposit(@NotNull @Positive BigDecimal amount) {
        EVENTS.publish(new AccountEvent.Deposited(id, amount));
        return new Account(id, version, owner, balance.add(amount));
    }

    @Update(path = "/withdrawals", method = "POST")
    public Account withdraw(@NotNull @Positive BigDecimal amount) {
        if (amount.compareTo(balance) > 0) {
            throw new ConflictException("Insufficient balance");
        }
        EVENTS.publish(new AccountEvent.Withdrawn(id, amount));
        return new Account(id, version, owner, balance.subtract(amount));
    }
}
