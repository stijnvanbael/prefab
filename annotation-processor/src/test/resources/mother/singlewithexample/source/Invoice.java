package mother.singlewithexample.source;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Example;
import be.appify.prefab.core.annotations.rest.Create;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Invoice(
        @Id String id,
        @Version long version,
        InvoiceNumber invoiceNumber) {

    /**
     * Single-value type whose inner field carries an {@code @Example}.
     */
    public record InvoiceNumber(@Example("INV-001") String value) {
    }

    @Create
    public Invoice(InvoiceNumber invoiceNumber) {
        this(null, 0L, invoiceNumber);
    }
}

