package be.appify.prefab.example.avro.cashregister;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.example.avro.sale.Sale;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

import static be.appify.prefab.core.annotations.rest.HttpMethod.POST;

@Aggregate
@DbMigration
@GetById
public record CashRegister(
        @Id String id,
        @Version long version,
        @NotNull String name,
        double cashInDrawer
) implements PublishesEvents {
    @PersistenceCreator
    public CashRegister {
    }

    @Create
    public CashRegister(@NotNull String name) {
        this(UUID.randomUUID().toString(), 0L, name, 0.0);
        publish(new Created(id, name));
    }

    @Update(path = "/cash-in", method = POST)
    public CashRegister cashIn(double amount) {
        var newCashInDrawer = cashInDrawer + amount;
        publish(new CashedIn(id, amount));
        return new CashRegister(id, version, name, newCashInDrawer);
    }

    @Update(path = "cash-out", method = POST)
    public CashRegister cashOut(double amount) {
        var newCashInDrawer = cashInDrawer - amount;
        publish(new CashedOut(id, amount));
        return new CashRegister(id, version, name, newCashInDrawer);
    }

    @EventHandler
    @ByReference(property = "cashRegister")
    public CashRegister onSalePaid(Sale.Paid event) {
        if (event.payment().method() == Sale.PaymentMethod.CASH) {
            var newCashInDrawer = cashInDrawer + event.payment().amount();
            return new CashRegister(id, version, name, newCashInDrawer);
        }
        return this;
    }

    @Event(topic = "cash-register", serialization = Event.Serialization.AVRO)
    public sealed interface Events permits Created, CashedIn, CashedOut {
        @PartitioningKey
        String id();
    }

    public record Created(String id, String name) implements Events {
    }

    public record CashedIn(String id, double amount) implements Events {
    }

    public record CashedOut(String id, double amount) implements Events {
    }
}
