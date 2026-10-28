package be.appify.prefab.example.streams.sales;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.example.streams.BiddingZoneId;

@Event(topic = "hourly-zone-sales-total")
public record HourlyZoneSalesTotal(BiddingZoneId biddingZone, double total, int numberOfSales)
        implements Keyed<BiddingZoneId> {
    public static HourlyZoneSalesTotal empty() {
        return new HourlyZoneSalesTotal(null, 0, 0);
    }

    public HourlyZoneSalesTotal add(EnergySale sale) {
        return new HourlyZoneSalesTotal(sale.biddingZone(), total + sale.amount(), numberOfSales + 1);
    }

    @Override
    public BiddingZoneId key() {
        return biddingZone;
    }
}
