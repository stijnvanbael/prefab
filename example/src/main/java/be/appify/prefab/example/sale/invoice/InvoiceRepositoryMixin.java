package be.appify.prefab.example.sale.invoice;

import be.appify.prefab.core.annotations.RepositoryMixin;
import org.springframework.data.jdbc.repository.query.Query;

import java.util.Optional;

@RepositoryMixin(Invoice.class)
public interface InvoiceRepositoryMixin {
    @Query("""
            SELECT MAX(invoice_number) FROM invoice
            WHERE invoice_number LIKE :year || '%'
            """)
    Optional<String> findMaxInvoiceNumberForYear(String year);
}
