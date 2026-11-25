package com.example.loyalty;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record LoyaltyAccount(
        @Id Reference<LoyaltyAccount> id,
        @Version long version,
        @NotBlank String holder,
        int points,
        Tier tier
) {
    static final int GOLD_POINTS = 1000;

    private static final EventPublisher<RedemptionDecision> DECISIONS = EventPublisher.bind(RedemptionDecision.class);

    @Create
    public LoyaltyAccount(@NotBlank String holder) {
        this(Reference.create(), 0L, holder, 0, Tier.BASIC);
    }

    @EventHandler
    @ByReference(property = "account")
    public LoyaltyAccount onPointsEarned(PointsEarned event) {
        var total = points + event.points();
        return new LoyaltyAccount(id, version, holder, total, total >= GOLD_POINTS ? Tier.GOLD : tier);
    }

    @EventHandler
    @ByReference(property = "account")
    public LoyaltyAccount onRedemptionRequested(RedemptionRequested request) {
        if (request.points() > points) {
            DECISIONS.publish(new RedemptionDecision.Rejected(request.redemption()));
            return this;
        }
        DECISIONS.publish(new RedemptionDecision.Approved(request.redemption()));
        return new LoyaltyAccount(id, version, holder, points - request.points(), tier);
    }
}
