package com.example.auctions;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import java.math.BigDecimal;

@Event(topic = "auctions")
public record AuctionClosed(@PartitioningKey Reference<Auction> auction, String item, Reference<Bid> winningBid,
        BigDecimal amount) {
}
