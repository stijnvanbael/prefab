package be.appify.prefab.example.sale;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.EventHandler;

import java.math.BigDecimal;
import java.util.Optional;

@Aggregate
@DbMigration(version = 5)
public class Invoice {
    private final BigDecimal total;

    public Invoice(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal total() {
        return total;
    }

    @EventHandler
    public static Optional<Invoice> onSaleCompleted(SaleCompleted event) {
        if (event.sale().type() == SaleType.INVOICE) {
            return Optional.of(new Invoice(event.sale().total()));
        }
        return Optional.empty();
    }
}
