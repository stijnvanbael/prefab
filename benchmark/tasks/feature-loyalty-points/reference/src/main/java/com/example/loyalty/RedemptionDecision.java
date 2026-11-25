package com.example.loyalty;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@Event(topic = "redemption-decisions")
public sealed interface RedemptionDecision permits RedemptionDecision.Approved, RedemptionDecision.Rejected {
    @PartitioningKey
    Reference<Redemption> redemption();

    record Approved(Reference<Redemption> redemption) implements RedemptionDecision {
    }

    record Rejected(Reference<Redemption> redemption) implements RedemptionDecision {
    }
}
