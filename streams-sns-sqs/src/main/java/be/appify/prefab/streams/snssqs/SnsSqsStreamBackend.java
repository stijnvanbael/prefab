package be.appify.prefab.streams.snssqs;

import be.appify.prefab.streams.StreamBackend;

/** The AWS SNS/SQS backend of Prefab Streams. */
public final class SnsSqsStreamBackend {
    /** Identifier of the SNS/SQS backend. */
    public static final StreamBackend SNS_SQS = StreamBackend.named("sns-sqs");

    private SnsSqsStreamBackend() {
    }
}
