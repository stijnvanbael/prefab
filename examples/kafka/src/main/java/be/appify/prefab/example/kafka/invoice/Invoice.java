package be.appify.prefab.example.kafka.invoice;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/** An invoice that can be listed with text and range filters. */
@Aggregate
@GetById
@GetList
public record Invoice(
        @Id Reference<Invoice> id,
        @Version long version,
        @Filter String customer,
        @Filter(operator = Filter.Operator.GREATER_THAN_OR_EQUAL)
        @Filter(operator = Filter.Operator.LESS_THAN_OR_EQUAL) BigDecimal amount,
        @Filter(operator = Filter.Operator.GREATER_THAN)
        @Filter(operator = Filter.Operator.LESS_THAN) LocalDate dueDate,
        @Filter(operator = Filter.Operator.GREATER_THAN_OR_EQUAL) int quantity,
        @Filter(operator = Filter.Operator.LESS_THAN) Instant receivedAt
) {

    /** Creates a new invoice. */
    @Create
    public Invoice(String customer, BigDecimal amount, LocalDate dueDate, int quantity, Instant receivedAt) {
        this(Reference.create(), 0L, customer, amount, dueDate, quantity, receivedAt);
    }
}
