package be.appify.prefab.streams.snssqs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SnsSqsNamesTest {

    @Test
    void queueNameCombinesApplicationAndSourceNode() {
        assertThat(SnsSqsNames.queueName("shop", "source-order-events")).isEqualTo("shop-streams-source-order-events");
    }

    @Test
    void queueNameIsSanitizedToTheCharactersSqsAllows() {
        assertThat(SnsSqsNames.queueName("my shop", "source-orders.v1")).isEqualTo("my-shop-streams-source-orders-v1");
    }

    @Test
    void longQueueNamesAreShortenedWithAStableHash() {
        var node = "source-" + "very-long-topic-name-".repeat(5);

        var name = SnsSqsNames.queueName("shop", node);

        assertThat(name).hasSize(75).isEqualTo(SnsSqsNames.queueName("shop", node));
        assertThat(name).isNotEqualTo(SnsSqsNames.queueName("shop", node + "x"));
    }

    @Test
    void shortKeysAreUsedAsMessageGroupId() {
        assertThat(SnsSqsNames.messageGroupId("order-1")).isEqualTo("order-1");
    }

    @Test
    void longKeysAreHashedToFitTheMessageGroupLimit() {
        var groupId = SnsSqsNames.messageGroupId("k".repeat(300));

        assertThat(groupId).hasSize(64);
        assertThat(groupId).isEqualTo(SnsSqsNames.messageGroupId("k".repeat(300)));
    }
}
