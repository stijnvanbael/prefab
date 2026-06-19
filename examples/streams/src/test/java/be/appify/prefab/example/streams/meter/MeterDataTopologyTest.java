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
    void shouldPublishFirstMeterDataForMeter() {
        try (var test = bootstrap.run(topology)) {
            var input = test.input(RawMeterData.class);
            var output = test.output(MeterData.class);

            var key = new MeterSerialNumber("1SAG456789");
            var rawMeterData = new RawMeterData(
                    key,
                    "001.xml",
                    Instant.now(),
                    Instant.parse("2026-01-01T00:00:00+01:00"),
                    Duration.ofHours(1),
                    List.of(0.3, 0.4, 0.2, 1.5, 1.7, 2.1),
                    24
            );
            input.pipeInput(key, rawMeterData);

            assertThat(output.readValuesToList())
                    .satisfiesExactly(meterData -> Assertions.assertThat(meterData)
                            .hasKey(key.value())
                            .hasStart(rawMeterData.start())
                            .hasInterval(rawMeterData.interval())
                            .hasValuesSatisfying(values -> values.containsExactlyElementsOf(rawMeterData.values())));
        }
    }

    @Test
    void shouldPublishOnlyNewMeterDataForMeter() {
        try (var test = bootstrap.run(topology)) {
            var input = test.input(RawMeterData.class);
            var output = test.output(MeterData.class);

            var key = new MeterSerialNumber("1SAG456789");
            var fileTimestamp = Instant.now();
            var rawMeterData1 = new RawMeterData(
                    key,
                    "001.xml",
                    fileTimestamp,
                    Instant.parse("2026-01-01T00:00:00+01:00"),
                    Duration.ofHours(1),
                    List.of(0.3, 0.4, 0.2, 1.5, 1.7, 2.1),
                    24
            );
            input.pipeInput(key, rawMeterData1);

            var rawMeterData2 = new RawMeterData(
                    key,
                    "001.xml",
                    fileTimestamp,
                    Instant.parse("2026-01-01T03:00:00+01:00"),
                    Duration.ofHours(1),
                    List.of(1.5, 1.7, 2.1, 3.5, 3.7, 2.9),
                    24
            );
            input.pipeInput(key, rawMeterData2);

            assertThat(output.readValuesToList())
                    .satisfiesExactly(
                            meterData -> Assertions.assertThat(meterData)
                                    .hasKey(key.value())
                                    .hasStart(rawMeterData1.start())
                                    .hasInterval(rawMeterData1.interval())
                                    .hasValuesSatisfying(values -> values.containsExactlyElementsOf(rawMeterData1.values())),
                            meterData -> Assertions.assertThat(meterData)
                                    .hasKey(key.value())
                                    .hasStart(rawMeterData2.start())
                                    .hasInterval(rawMeterData2.interval())
                                    .hasValuesSatisfying(values -> values.containsExactlyElementsOf(List.of(3.5, 3.7, 2.9)))
                    );
        }
    }
}
