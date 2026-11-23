package com.example.payments;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import com.example.ordering.Order;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Payment(
        @Id Reference<Payment> id,
        @Version long version,
        @NotNull Reference<Order> order,
        @NotNull BigDecimal amount
) {
    private static final EventPublisher<PaymentReceived> RECEIVED = EventPublisher.bind(PaymentReceived.class);

    @Create
    public Payment(@NotNull Reference<Order> order, @NotNull @Positive BigDecimal amount) {
        this(Reference.create(), 0L, order, amount);
        RECEIVED.publish(new PaymentReceived(order, amount));
    }
}
