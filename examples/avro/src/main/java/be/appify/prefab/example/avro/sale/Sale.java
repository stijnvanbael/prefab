package be.appify.prefab.example.avro.sale;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.avro.cashregister.CashRegister;
import be.appify.prefab.example.avro.customer.Customer;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import static be.appify.prefab.core.annotations.rest.HttpMethod.POST;

@Aggregate
@DbMigration
public record Sale(
        @Id String id,
        @Version long version,
        @NotNull Instant started,
        @NotNull List<Line> lines,
        @NotNull Status status,
        @NotNull Reference<CashRegister> cashRegister,
        Reference<Customer> customer,
        @Embedded.Nullable(prefix = "payment_") Payment payment
) implements PublishesEvents {
    @PersistenceCreator
    public Sale {
    }

    @Create
    public Sale(Reference<CashRegister> cashRegister) {
        this(UUID.randomUUID().toString(), 0L, Instant.now(), new ArrayList<>(), Status.OPEN, cashRegister, null, null);
        publish(new Created(id, started, cashRegister));
    }

    @Update(path = "/lines", method = POST)
    public Sale addLine(
            @NotNull String product,
            double quantity,
            double price
    ) {
        var newLines = new ArrayList<>(lines);
        var line = new Line(product, quantity, price);
        newLines.add(line);
        publish(new LineAdded(id, line));
        return new Sale(id, version, started, newLines, status, cashRegister, customer, payment);
    }

    @Update(path = "/customer")
    public Sale addCustomer(@NotNull Reference<Customer> customer) {
        publish(new CustomerAdded(id, customer));
        return new Sale(id, version, started, lines, status, cashRegister, customer, payment);
    }

    @Update(path = "/payment")
    public Sale pay(double amount, @NotNull PaymentMethod method) {
        var payment = new Payment(amount, method, Instant.now());
        publish(new Paid(id, lines, cashRegister, payment));
        return new Sale(id, version, started, lines, Status.CLOSED, cashRegister, customer, payment);
    }

    @Table("sale_line")
    public record Line(
            @NotNull String product,
            double quantity,
            double price
    ) {
    }

    public record Payment(
            double amount,
            @NotNull PaymentMethod method,
            @NotNull Instant paidAt
    ) {
    }

    public enum Status {
        OPEN,
        CLOSED
    }

    public enum PaymentMethod {
        CREDIT_CARD,
        CASH
    }

    @Event(topic = "sale", serialization = Event.Serialization.AVRO)
    public sealed interface Events permits Created, LineAdded, CustomerAdded, Paid {
        @PartitioningKey
        String id();
    }

    public record Created(String id, Instant started, Reference<CashRegister> cashRegister) implements Events {
    }

    public record LineAdded(String id, Line line) implements Events {
    }

    public record CustomerAdded(String id, Reference<Customer> customer) implements Events {
    }

    public record Paid(String id, List<Line> lines, Reference<CashRegister> cashRegister, Payment payment) implements Events {
    }
}
