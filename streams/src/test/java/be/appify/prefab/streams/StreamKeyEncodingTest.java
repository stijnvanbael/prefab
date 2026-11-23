package be.appify.prefab.streams;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StreamKeyEncodingTest {
    @Test
    void singleComponentRecordsUsePlainUtf8() {
        var encoding = new StreamKeyEncoding<>(SingleFieldKey.class);

        assertThat(encoding.encode(new SingleFieldKey("zone-1"))).isEqualTo("zone-1".getBytes(StandardCharsets.UTF_8));
        assertThat(encoding.decode("zone-1".getBytes(StandardCharsets.UTF_8))).isEqualTo(new SingleFieldKey("zone-1"));
    }

    @Test
    void otherKeyShapesUseJson() {
        var encoding = new StreamKeyEncoding<>(CompositeKey.class);

        var bytes = encoding.encode(new CompositeKey("zone-1", "order-2"));

        assertThat(new String(bytes, StandardCharsets.UTF_8))
                .contains("\"zone\":\"zone-1\"")
                .contains("\"order\":\"order-2\"");
        assertThat(encoding.decode(bytes)).isEqualTo(new CompositeKey("zone-1", "order-2"));
    }

    record SingleFieldKey(String value) {
    }

    record CompositeKey(String zone, String order) {
    }
}
