package kafka.multitopic;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.Multicast;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

@Aggregate
public record DayTotal(
        @Id String id,
        @Version long version,
        LocalDate date,
        Double salesTotal,
        Double refundsTotal
) {
    @PersistenceCreator
    public DayTotal {
    }

    @EventHandler
    @Multicast(queryMethod = "findByDate", parameters = "date")
    public DayTotal onSaleCreated(Sale.Created event) {
        return new DayTotal(
                id,
                version,
                date,
                salesTotal + event.total(),
                refundsTotal
        );
    }

    @EventHandler
    @Multicast(queryMethod = "findByDate", parameters = "date")
    public DayTotal onRefundCreated(Refund.Created event) {
        return new DayTotal(
                id,
                version,
                date,
                salesTotal,
                refundsTotal + event.total()
        );
    }
}