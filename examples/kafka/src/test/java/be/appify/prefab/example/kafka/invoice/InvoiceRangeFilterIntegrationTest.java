package be.appify.prefab.example.kafka.invoice;

import be.appify.prefab.example.kafka.invoice.infrastructure.http.InvoiceResponse;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class InvoiceRangeFilterIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Autowired
    InvoiceClient invoices;

    private final String customer = "customer-" + UUID.randomUUID();

    private void create(String suffix, String amount, String dueDate, int quantity, Instant receivedAt) throws Exception {
        invoices.createInvoice(customer + suffix, new BigDecimal(amount), LocalDate.parse(dueDate), quantity, receivedAt);
    }

    private void createSamples() throws Exception {
        create("-a", "10.00", "2026-02-01", 1, NOW.minusSeconds(3600));
        create("-b", "20.00", "2026-03-01", 5, NOW);
        create("-c", "30.00", "2026-04-01", 10, NOW.plusSeconds(3600));
        create("-d", "40.00", "2026-05-01", 20, NOW.plusSeconds(7200));
    }

    private FilterBuilder filter() {
        return new FilterBuilder();
    }

    private class FilterBuilder {
        private String customerFilter = customer;
        private BigDecimal amountMin;
        private BigDecimal amountMax;
        private LocalDate dueAfter;
        private LocalDate dueBefore;
        private Integer quantityMin;
        private Instant receivedBefore;
        private Pageable pageable = Pageable.unpaged();

        FilterBuilder customer(String value) {
            customerFilter = value;
            return this;
        }

        FilterBuilder amountBetween(String min, String max) {
            amountMin = min == null ? null : new BigDecimal(min);
            amountMax = max == null ? null : new BigDecimal(max);
            return this;
        }

        FilterBuilder dueBetween(String after, String before) {
            dueAfter = after == null ? null : LocalDate.parse(after);
            dueBefore = before == null ? null : LocalDate.parse(before);
            return this;
        }

        FilterBuilder quantityAtLeast(int value) {
            quantityMin = value;
            return this;
        }

        FilterBuilder receivedBefore(Instant value) {
            receivedBefore = value;
            return this;
        }

        FilterBuilder pageable(Pageable value) {
            pageable = value;
            return this;
        }

        be.appify.prefab.core.spring.Page<InvoiceResponse> run() throws Exception {
            return invoices.findInvoices(pageable, customerFilter, amountMin, amountMax, dueAfter, dueBefore,
                    quantityMin, receivedBefore);
        }

        java.util.List<String> suffixes() throws Exception {
            return run().content().stream()
                    .map(invoice -> invoice.customer().substring(customer.length()))
                    .sorted()
                    .toList();
        }
    }

    @Test
    void withoutRangeParametersReturnsAllMatchingTheTextFilter() throws Exception {
        createSamples();

        assertThat(filter().suffixes()).containsExactly("-a", "-b", "-c", "-d");
    }

    @Test
    void lowerBoundIsInclusiveForGreaterThanOrEqual() throws Exception {
        createSamples();

        assertThat(filter().amountBetween("20.00", null).suffixes()).containsExactly("-b", "-c", "-d");
    }

    @Test
    void upperBoundIsInclusiveForLessThanOrEqual() throws Exception {
        createSamples();

        assertThat(filter().amountBetween(null, "30.00").suffixes()).containsExactly("-a", "-b", "-c");
    }

    @Test
    void bothBoundsOnTheSameFieldActAsBetween() throws Exception {
        createSamples();

        assertThat(filter().amountBetween("20.00", "30.00").suffixes()).containsExactly("-b", "-c");
    }

    @Test
    void greaterThanAndLessThanAreExclusive() throws Exception {
        createSamples();

        assertThat(filter().dueBetween("2026-02-01", "2026-05-01").suffixes()).containsExactly("-b", "-c");
    }

    @Test
    void greaterThanOrEqualWorksOnIntegerFields() throws Exception {
        createSamples();

        assertThat(filter().quantityAtLeast(5).suffixes()).containsExactly("-b", "-c", "-d");
    }

    @Test
    void lessThanWorksOnInstantFields() throws Exception {
        createSamples();

        assertThat(filter().receivedBefore(NOW).suffixes()).containsExactly("-a");
    }

    @Test
    void multipleRangeFieldsAreCombinedWithAnd() throws Exception {
        createSamples();

        var result = filter()
                .amountBetween("15.00", "40.00")
                .dueBetween(null, "2026-05-01")
                .quantityAtLeast(10)
                .receivedBefore(NOW.plusSeconds(7200))
                .suffixes();

        assertThat(result).containsExactly("-c");
    }

    @Test
    void rangeIsCombinedWithTextFilter() throws Exception {
        createSamples();

        assertThat(filter().customer(customer + "-c").amountBetween("20.00", null).suffixes())
                .containsExactly("-c");
        assertThat(filter().customer(customer + "-a").amountBetween("20.00", null).suffixes()).isEmpty();
    }

    @Test
    void rangeWithoutTextFilterStillAppliesRange() throws Exception {
        createSamples();

        var amounts = filter().customer(null).amountBetween("40.00", "40.00").run().content().stream()
                .filter(invoice -> invoice.customer().startsWith(customer))
                .map(InvoiceResponse::amount)
                .toList();

        assertThat(amounts).hasSize(1);
        assertThat(amounts.getFirst()).isEqualByComparingTo("40.00");
    }

    @Test
    void noMatchReturnsEmptyPage() throws Exception {
        createSamples();

        var page = filter().amountBetween("100.00", null).run();

        assertThat(page.content()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void emptyRangeBecauseLowerBoundExceedsUpperBoundReturnsNothing() throws Exception {
        createSamples();

        assertThat(filter().amountBetween("30.00", "20.00").suffixes()).isEmpty();
    }

    @Test
    void pagingAndSortingApplyToRangeQueriesAndTotalCountsAllMatches() throws Exception {
        createSamples();
        var firstPage = filter()
                .amountBetween("20.00", null)
                .pageable(PageRequest.of(0, 2, Sort.by("amount").descending()))
                .run();
        var secondPage = filter()
                .amountBetween("20.00", null)
                .pageable(PageRequest.of(1, 2, Sort.by("amount").descending()))
                .run();

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.content()).extracting(invoice -> invoice.customer().substring(customer.length()))
                .containsExactly("-d", "-c");
        assertThat(secondPage.content()).extracting(invoice -> invoice.customer().substring(customer.length()))
                .containsExactly("-b");
    }
}
