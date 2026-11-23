package be.appify.prefab.streams.snssqs;

import be.appify.prefab.core.domain.Keyed;

/** Records shared by the tests of the SNS/SQS streams backend. */
final class StreamFixtures {
    private StreamFixtures() {
    }

    /** A sale of a category, keyed by its own ID. */
    record Sale(String saleId, String category, int amount) implements Keyed<String> {
        @Override
        public String key() {
            return saleId;
        }
    }

    /** The total amount sold in a category. */
    record CategoryTotal(String category, int total) implements Keyed<String> {
        @Override
        public String key() {
            return category;
        }
    }

    /** A product that is looked up by an enrichment. */
    record Product(String category, String label) implements Keyed<String> {
        @Override
        public String key() {
            return category;
        }
    }

    /** A refund of a sale, keyed by the sale it refunds. */
    record Refund(String saleId, String reason) implements Keyed<String> {
        @Override
        public String key() {
            return saleId;
        }
    }

    /** A sale together with its refund. */
    record RefundedSale(String saleId, String category, String reason) implements Keyed<String> {
        @Override
        public String key() {
            return saleId;
        }
    }
}
