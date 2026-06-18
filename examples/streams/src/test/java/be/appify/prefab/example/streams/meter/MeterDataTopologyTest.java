package be.appify.prefab.example.streams.meter;

import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.test.streams.kafka.KafkaTopologyTestBootstrap;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MeterDataTopologyTest {
    private final KafkaTopologyTestBootstrap bootstrap = KafkaTopologyTestBootstrap.bootstrap();
    private final PrefabStreams streams = bootstrap.streams();
    private final StreamDefinition topology = new MeterDataTopology().topology(streams);

    @Test
    void shouldPublishFirstMeterDataFromFile() {
        try (var test = bootstrap.run(topology)) {
            var input = test.input(RawMeterData.class);
            var output = test.output(MeterData.class);

            var key = new RawMeterDataKey(new MeterSerialNumber("1SAG456789"), "001.xml", Instant.now());
            var rawMeterData = new RawMeterData(
                    key,
                    Instant.parse("2026-01-01T00:00:00+01:00"),
                    Duration.ofHours(1),
                    List.of(0.3, 0.4, 0.2, 1.5, 1.7, 2.1),
                    24
            );
            input.pipeInput(key, rawMeterData);

            assertThat(output.readValuesToList())
                    .satisfiesExactly(meterData -> Assertions.assertThat(meterData)
                            .hasKey(rawMeterData.key().meterSerialNumber().value())
                            .hasStart(rawMeterData.start())
                            .hasInterval(rawMeterData.interval())
                            .hasValuesSatisfying(values -> values.containsExactlyElementsOf(rawMeterData.values())));
        }
    }
}
