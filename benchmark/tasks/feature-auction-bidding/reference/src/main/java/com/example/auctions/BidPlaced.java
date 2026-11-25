package com.example.auctions;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import java.math.BigDecimal;

@Event(topic = "bids")
public record BidPlaced(@PartitioningKey Reference<Auction> auction, Reference<Bid> bid, BigDecimal amount) {
}
