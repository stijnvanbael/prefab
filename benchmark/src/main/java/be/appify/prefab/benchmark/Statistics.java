package be.appify.prefab.benchmark;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/** Small statistics helpers for reports. */
final class Statistics {
    private static final double Z_95 = 1.959964;

    private Statistics() {
    }

    /**
     * Wilson score interval for a pass rate, which behaves well for small samples and rates near 0 or 1.
     *
     * @param passed number of passes
     * @param total  number of attempts
     * @return the observed rate with its 95% confidence interval
     */
    static Interval wilson(long passed, long total) {
        if (total == 0) {
            return new Interval(0, 0, 0);
        }
        double p = (double) passed / total;
        double z2 = Z_95 * Z_95;
        double denominator = 1 + z2 / total;
        double centre = (p + z2 / (2.0 * total)) / denominator;
        double margin = Z_95 * Math.sqrt(p * (1 - p) / total + z2 / (4.0 * total * total)) / denominator;
        return new Interval(p, Math.max(0, centre - margin), Math.min(1, centre + margin));
    }

    static double median(List<? extends Number> values) {
        if (values.isEmpty()) {
            return 0;
        }
        var sorted = values.stream().mapToDouble(Number::doubleValue).sorted().toArray();
        int middle = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
    }

    static String formatDuration(Duration duration) {
        long seconds = duration.toSeconds();
        return seconds >= 60 ? String.format(Locale.ROOT, "%dm%02ds", seconds / 60, seconds % 60) : seconds + "s";
    }

    /**
     * A rate with a confidence interval.
     *
     * @param rate  observed rate
     * @param lower lower bound
     * @param upper upper bound
     */
    record Interval(double rate, double lower, double upper) {
        String format() {
            return String.format(Locale.ROOT, "%.0f%% (95%% CI %.0f-%.0f%%)", rate * 100, lower * 100, upper * 100);
        }
    }

}
