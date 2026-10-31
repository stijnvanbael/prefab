package be.appify.prefab.core.serialization;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Deterministic broker-safe names for independent concrete event subscriptions. */
public final class EventSubscriptionNames {
    private EventSubscriptionNames() {}

    /**
     * Names a subscription by its complete owner, concrete type and resolved topic.
     * The full digest fits SQS's 80-character limit without truncating event identity.
     *
     * @param owner qualified handler owner name
     * @param eventType canonical concrete event identity
     * @param topic resolved source topic
     * @return a broker-safe subscription or queue name
     */
    public static String name(String owner, String eventType, String topic) {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest((owner + "\n" + eventType + "\n" + topic).getBytes(StandardCharsets.UTF_8));
            return "prefab-" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
