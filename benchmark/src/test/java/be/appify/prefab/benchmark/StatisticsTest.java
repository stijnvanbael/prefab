package be.appify.prefab.benchmark;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StatisticsTest {

    @ParameterizedTest
    @CsvSource({
            "15, 30, 0.5, 0.332, 0.668",
            "30, 30, 1.0, 0.886, 1.0",
            "0, 30, 0.0, 0.0, 0.114",
            "27, 30, 0.9, 0.744, 0.965"
    })
    void wilsonIntervalMatchesReferenceValues(long passed, long total, double rate, double lower, double upper) {
        var interval = Statistics.wilson(passed, total);

        assertThat(interval.rate()).isCloseTo(rate, within(0.001));
        assertThat(interval.lower()).isCloseTo(lower, within(0.002));
        assertThat(interval.upper()).isCloseTo(upper, within(0.002));
    }

    @Test
    void emptySampleHasZeroRate() {
        assertThat(Statistics.wilson(0, 0)).isEqualTo(new Statistics.Interval(0, 0, 0));
    }

    @Test
    void medianAndDurationFormatting() {
        assertThat(Statistics.median(List.of(3, 1, 2))).isEqualTo(2);
        assertThat(Statistics.median(List.of(4, 1, 2, 3))).isEqualTo(2.5);
        assertThat(Statistics.formatDuration(Duration.ofSeconds(125))).isEqualTo("2m05s");
        assertThat(Statistics.formatDuration(Duration.ofSeconds(9))).isEqualTo("9s");
    }
}
