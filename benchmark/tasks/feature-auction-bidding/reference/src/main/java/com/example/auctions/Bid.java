package com.example.auctions;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.audit.CreatedBy;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Bid(
        @Id Reference<Bid> id,
        @Version long version,
        @CreatedBy String bidder,
        @NotNull Reference<Auction> auction,
        @NotNull @Positive BigDecimal amount
) {
    private static final EventPublisher<BidPlaced> PLACED = EventPublisher.bind(BidPlaced.class);

    @Create
    public Bid(@NotNull Reference<Auction> auction, @NotNull @Positive BigDecimal amount) {
        this(Reference.create(), 0L, null, auction, amount);
        PLACED.publish(new BidPlaced(auction, id, amount));
    }
}
