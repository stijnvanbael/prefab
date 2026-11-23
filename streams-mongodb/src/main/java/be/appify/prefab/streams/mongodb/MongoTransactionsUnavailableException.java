package be.appify.prefab.streams.mongodb;

/**
 * Thrown at startup when the MongoDB deployment cannot run multi-document transactions, which the stream state
 * repository needs to commit state, inbox and outbox atomically. Standalone servers do not support them; replica sets
 * and sharded clusters do.
 */
public class MongoTransactionsUnavailableException extends IllegalStateException {
    /** Creates the exception. */
    public MongoTransactionsUnavailableException() {
        super("The MongoDB deployment does not support transactions: it is a standalone server, not a replica set "
                + "or sharded cluster. The Prefab Streams state repository needs transactions.");
    }
}
