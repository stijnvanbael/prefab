package be.appify.prefab.example.streams.sales;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.example.streams.BiddingZoneId;

@Event(topic = "zone-sales-total")
public record ZoneSalesTotal(BiddingZoneId biddingZone, double total, int numberOfSales)
        implements Keyed<BiddingZoneId> {
    public static ZoneSalesTotal empty() {
        return new ZoneSalesTotal(null, 0, 0);
    }

    public ZoneSalesTotal add(EnergySale sale) {
        return new ZoneSalesTotal(sale.biddingZone(), total + sale.amount(), numberOfSales + 1);
    }

    @Override
    public BiddingZoneId key() {
        return biddingZone;
    }
}
