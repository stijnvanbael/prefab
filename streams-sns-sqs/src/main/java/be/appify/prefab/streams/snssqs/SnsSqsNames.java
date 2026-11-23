package be.appify.prefab.streams.snssqs;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Naming rules for the SQS queues and SNS message groups of the streams backend. */
final class SnsSqsNames {
    /** An SQS queue name has at most 80 characters, five of which the {@code .fifo} suffix may need. */
    private static final int MAX_QUEUE_STEM = 75;
    /** The hash that keeps a shortened name unique. */
    private static final int HASH_LENGTH = 8;
    /** An SNS message group ID has at most 128 characters. */
    private static final int MAX_GROUP_ID = 128;

    private SnsSqsNames() {
    }

    /**
     * Names the queue of a source node: {@code {application}-streams-{node}}, sanitized to the characters SQS allows
     * and shortened with a hash when it is too long. All instances of an application use the same name, so they
     * share the queue.
     */
    static String queueName(String application, String node) {
        var name = (application + "-streams-" + node).replaceAll("[^a-zA-Z0-9_-]", "-");
        if (name.length() <= MAX_QUEUE_STEM) {
            return name;
        }
        return name.substring(0, MAX_QUEUE_STEM - HASH_LENGTH - 1) + "-" + hash(name).substring(0, HASH_LENGTH);
    }

    /** Returns the message group ID for a key: the key itself, or a hash of it when it is too long. */
    static String messageGroupId(String encodedKey) {
        return encodedKey.length() <= MAX_GROUP_ID ? encodedKey : hash(encodedKey);
    }

    private static String hash(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
