package com.example.loyalty;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Redemption(
        @Id Reference<Redemption> id,
        @Version long version,
        @NotNull Reference<LoyaltyAccount> account,
        @Positive int points,
        RedemptionStatus status
) {
    private static final EventPublisher<RedemptionRequested> REQUESTED =
            EventPublisher.bind(RedemptionRequested.class);

    @Create(security = @Security(role = "CASHIER"))
    public Redemption(@NotNull Reference<LoyaltyAccount> account, @Positive int points) {
        this(Reference.create(), 0L, account, points, RedemptionStatus.PENDING);
        REQUESTED.publish(new RedemptionRequested(account, id, points));
    }

    @EventHandler
    @ByReference(property = "redemption")
    public Redemption onApproved(RedemptionDecision.Approved decision) {
        return new Redemption(id, version, account, points, RedemptionStatus.APPROVED);
    }

    @EventHandler
    @ByReference(property = "redemption")
    public Redemption onRejected(RedemptionDecision.Rejected decision) {
        return new Redemption(id, version, account, points, RedemptionStatus.REJECTED);
    }
}
