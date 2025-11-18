package be.appify.prefab.example.sale.invoice;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.example.sale.SaleCompleted;
import be.appify.prefab.example.sale.SaleType;
import be.appify.prefab.example.sale.invoice.application.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Component
public class CreateInvoiceUseCase {
    private final InvoiceRepository invoiceRepository;

    public CreateInvoiceUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @EventHandler
    public void onSaleCompleted(SaleCompleted event) {
        if (event.type() == SaleType.INVOICE) {
            var invoice = new Invoice(
                    UUID.randomUUID().toString(),
                    0,
                    calculateTotal(event),
                    Instant.now(),
                    nextInvoiceNumber(event)
            );
            invoiceRepository.save(invoice);
        }
    }

    private String nextInvoiceNumber(SaleCompleted event) {
        var year = String.valueOf(event.start().atZone(ZoneId.systemDefault()).getYear());
        return invoiceRepository.findMaxInvoiceNumberForYear(year)
                .map(max -> Integer.parseInt(max) + 1)
                .orElseGet(() -> Integer.parseInt(year + "0001"))
                .toString();
    }

    private static BigDecimal calculateTotal(SaleCompleted event) {
        return event.items().stream()
                .map(saleItem -> saleItem.price().multiply(saleItem.quantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
