package be.appify.prefab.example.streams.meter;

import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TopologyTestExtension.class)
class MeterDataTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @BeforeEach
    void declareTopology() {
        new MeterDataTopology().topology(streams);
    }

    @Test
    void shouldPublishFirstMeterDataForMeter() {
        var input = topology.input(RawMeterData.class);
        var output = topology.output(MeterData.class);

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
        input.send(rawMeterData);

        assertThat(output.values())
                .satisfiesExactly(meterData -> Assertions.assertThat(meterData)
                        .hasKey(key.value())
                        .hasStart(rawMeterData.start())
                        .hasInterval(rawMeterData.interval())
                        .hasValuesSatisfying(values -> values.containsExactlyElementsOf(rawMeterData.values())));
    }

    @Test
    void shouldPublishOnlyNewMeterDataForMeter() {
        var input = topology.input(RawMeterData.class);
        var output = topology.output(MeterData.class);

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
        input.send(rawMeterData1);

        var rawMeterData2 = new RawMeterData(
                key,
                "001.xml",
                fileTimestamp,
                Instant.parse("2026-01-01T03:00:00+01:00"),
                Duration.ofHours(1),
                List.of(1.5, 1.7, 2.1, 3.5, 3.7, 2.9),
                24
        );
        input.send(rawMeterData2);

        assertThat(output.values())
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
