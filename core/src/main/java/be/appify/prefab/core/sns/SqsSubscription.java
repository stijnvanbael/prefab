package be.appify.prefab.core.sns;

/** Handle for a running SQS subscription. */
@FunctionalInterface
public interface SqsSubscription extends AutoCloseable {

    /** Stops polling the queue. Messages that are being handled are not acknowledged. */
    @Override
    void close();
}
