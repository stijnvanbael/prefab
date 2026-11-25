package com.example.loyalty;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Purchase(
        @Id Reference<Purchase> id,
        @Version long version,
        @NotNull Reference<LoyaltyAccount> account,
        @NotNull @Positive BigDecimal amount
) {
    private static final EventPublisher<PointsEarned> EARNED = EventPublisher.bind(PointsEarned.class);

    @Create(security = @Security(role = "CASHIER"))
    public Purchase(@NotNull Reference<LoyaltyAccount> account, @NotNull @Positive BigDecimal amount) {
        this(Reference.create(), 0L, account, amount);
        EARNED.publish(new PointsEarned(account, amount.intValue()));
    }
}
