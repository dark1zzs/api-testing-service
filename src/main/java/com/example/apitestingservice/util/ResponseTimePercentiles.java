package com.example.apitestingservice.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Linear interpolation on sorted sample (same family as Excel PERCENTILE.INC / R type 7).
 * Used for project-level latency summaries over all stored test runs.
 */
public final class ResponseTimePercentiles {

    private ResponseTimePercentiles() {
    }

    public record Result(long sampleCount, Long p50Ms, Long p95Ms) {
    }

    public static Result fromMillis(List<Long> responseTimesMs) {
        List<Long> values = new ArrayList<>();
        for (Long ms : responseTimesMs) {
            if (ms != null && ms >= 0) {
                values.add(ms);
            }
        }
        if (values.isEmpty()) {
            return new Result(0, null, null);
        }
        Collections.sort(values);
        long n = values.size();
        return new Result(
                n,
                percentileInclusive(values, 50.0),
                percentileInclusive(values, 95.0)
        );
    }

    private static Long percentileInclusive(List<Long> sorted, double p) {
        if (sorted.isEmpty()) {
            return null;
        }
        if (sorted.size() == 1) {
            return sorted.getFirst();
        }
        double rank = (p / 100.0) * (sorted.size() - 1);
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);
        double weight = rank - lower;
        double v = sorted.get(lower) * (1.0 - weight) + sorted.get(upper) * weight;
        return Math.round(v);
    }
}
