package com.example.loyalty;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

@Event(topic = "points-earned")
public record PointsEarned(@PartitioningKey Reference<LoyaltyAccount> account, int points) {
}
