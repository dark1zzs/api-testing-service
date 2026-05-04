package com.example.apitestingservice.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResponseTimePercentilesTest {

    @Test
    void shouldReturnNullPercentilesWhenNoSamples() {
        ResponseTimePercentiles.Result result = ResponseTimePercentiles.fromMillis(List.of());

        assertEquals(0, result.sampleCount());
        assertNull(result.p50Ms());
        assertNull(result.p95Ms());
    }

    @Test
    void shouldIgnoreNullAndNegativeValues() {
        ResponseTimePercentiles.Result result = ResponseTimePercentiles.fromMillis(
                Arrays.asList(null, -1L, 100L)
        );

        assertEquals(1, result.sampleCount());
        assertEquals(100L, result.p50Ms());
        assertEquals(100L, result.p95Ms());
    }

    @Test
    void shouldComputeP50AndP95OnSortedSample() {
        ResponseTimePercentiles.Result result = ResponseTimePercentiles.fromMillis(
                List.of(100L, 200L, 300L, 400L, 1000L)
        );

        assertEquals(5, result.sampleCount());
        assertEquals(300L, result.p50Ms());
        assertEquals(880L, result.p95Ms());
    }
}
