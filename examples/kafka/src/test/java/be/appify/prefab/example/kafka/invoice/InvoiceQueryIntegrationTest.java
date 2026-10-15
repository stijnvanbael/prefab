package be.appify.prefab.example.kafka.invoice;

import be.appify.prefab.example.kafka.invoice.application.InvoiceQuery;
import be.appify.prefab.example.kafka.invoice.application.InvoiceService;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class InvoiceQueryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Autowired
    InvoiceClient invoices;

    @Autowired
    InvoiceService service;

    private final String customer = "customer-" + UUID.randomUUID();

    private void createSamples() throws Exception {
        invoices.createInvoice(customer + "-a", new BigDecimal("10.00"), LocalDate.parse("2026-02-01"), 1, NOW);
        invoices.createInvoice(customer + "-b", new BigDecimal("20.00"), LocalDate.parse("2026-03-01"), 5, NOW);
        invoices.createInvoice(customer + "-c", new BigDecimal("30.00"), LocalDate.parse("2026-04-01"), 10, NOW);
    }

    private List<String> suffixes(InvoiceQuery query) {
        return service.getList(query).getContent().stream()
                .map(invoice -> invoice.customer().substring(customer.length()))
                .toList();
    }

    @Test
    void unsetFiltersAreIgnored() throws Exception {
        createSamples();

        assertThat(suffixes(InvoiceQuery.newQuery().customer(customer).sort(Sort.by("customer"))))
                .containsExactly("-a", "-b", "-c");
    }

    @Test
    void rangeAndEqualityFiltersAreCombined() throws Exception {
        createSamples();

        var query = InvoiceQuery.newQuery()
                .customer(customer)
                .amountGreaterThanOrEqual(new BigDecimal("20.00"))
                .quantityGreaterThanOrEqual(10);

        assertThat(suffixes(query)).containsExactly("-c");
    }

    @Test
    void pagingAndSortingAreApplied() throws Exception {
        createSamples();

        var query = InvoiceQuery.newQuery()
                .customer(customer)
                .sort(Sort.by(Sort.Direction.DESC, "amount"))
                .page(1)
                .pageSize(2);

        var page = service.getList(query);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(suffixes(query)).containsExactly("-a");
    }
}
