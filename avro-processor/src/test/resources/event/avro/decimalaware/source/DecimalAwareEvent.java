package event.avro.decimalaware;

import be.appify.prefab.core.annotations.Event;
import java.math.BigDecimal;
import java.util.List;

@Event(topic = "decimal-aware", serialization = Event.Serialization.AVRO)
public record DecimalAwareEvent(
        BigDecimal amount,
        BigDecimal optionalAmount,
        List<BigDecimal> amounts) {
}
