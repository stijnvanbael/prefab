package com.example.loyalty;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

@Event(topic = "redemption-requests")
public record RedemptionRequested(@PartitioningKey Reference<LoyaltyAccount> account,
        Reference<Redemption> redemption, int points) {
}
