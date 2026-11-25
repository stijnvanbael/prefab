package com.example.auctions;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.audit.CreatedBy;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Auction(
        @Id Reference<Auction> id,
        @Version long version,
        @CreatedBy String seller,
        @NotBlank String item,
        @NotNull @Positive BigDecimal startingPrice,
        AuctionStatus status,
        int bidCount,
        @Nullable BigDecimal highestBid,
        @Nullable Reference<Bid> winningBid
) {
    private static final EventPublisher<AuctionClosed> CLOSED = EventPublisher.bind(AuctionClosed.class);

    @Create(security = @Security(role = "SELLER"))
    public Auction(@NotBlank String item, @NotNull @Positive BigDecimal startingPrice) {
        this(Reference.create(), 0L, null, item, startingPrice, AuctionStatus.OPEN, 0, null, null);
    }

    @EventHandler
    @ByReference(property = "auction")
    public Auction onBidPlaced(BidPlaced bid) {
        if (!accepts(bid.amount())) {
            return this;
        }
        return new Auction(id, version, seller, item, startingPrice, status, bidCount + 1, bid.amount(), bid.bid());
    }

    @Update(path = "/closing", method = "POST", security = @Security(role = "SELLER"))
    public Auction close() {
        if (status == AuctionStatus.CLOSED) {
            return this;
        }
        if (highestBid != null) {
            CLOSED.publish(new AuctionClosed(id, item, winningBid, highestBid));
        }
        return new Auction(id, version, seller, item, startingPrice, AuctionStatus.CLOSED, bidCount, highestBid,
                winningBid);
    }

    private boolean accepts(BigDecimal amount) {
        return status == AuctionStatus.OPEN
                && amount.compareTo(startingPrice) >= 0
                && (highestBid == null || amount.compareTo(highestBid) > 0);
    }
}
