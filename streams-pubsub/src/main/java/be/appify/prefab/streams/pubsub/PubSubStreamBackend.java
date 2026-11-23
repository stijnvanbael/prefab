package be.appify.prefab.streams.pubsub;

import be.appify.prefab.streams.StreamBackend;

/** Identifier for the Pub/Sub Streams backend. */
public final class PubSubStreamBackend {
    /** Pub/Sub backend identifier. */
    public static final StreamBackend PUBSUB = StreamBackend.named("pubsub");

    private PubSubStreamBackend() {
    }
}
