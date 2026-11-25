package com.example.auctions;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
public record Sale(
        @Id Reference<Sale> id,
        @Version long version,
        @Filter Reference<Auction> auction,
        String item,
        Reference<Bid> bid,
        BigDecimal amount
) {
    @EventHandler
    public static Sale onAuctionClosed(AuctionClosed event) {
        return new Sale(Reference.create(), 0L, event.auction(), event.item(), event.winningBid(), event.amount());
    }
}
