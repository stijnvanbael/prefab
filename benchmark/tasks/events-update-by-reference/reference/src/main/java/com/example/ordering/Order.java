package com.example.ordering;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.DbDefaultValue;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import com.example.payments.PaymentReceived;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        @NotNull String customer,
        @NotNull BigDecimal total,
        @NotNull OrderStatus status,
        @DbDefaultValue("0") @NotNull BigDecimal amountPaid
) {
    @Create
    public Order(@NotNull String customer, @NotNull BigDecimal total) {
        this(Reference.create(), 0L, customer, total, OrderStatus.OPEN, BigDecimal.ZERO);
    }

    @EventHandler
    @ByReference(property = "order")
    public Order onPaymentReceived(PaymentReceived payment) {
        var paid = amountPaid.add(payment.amount());
        return new Order(id, version, customer, total,
                paid.compareTo(total) >= 0 ? OrderStatus.PAID : OrderStatus.OPEN, paid);
    }
}
