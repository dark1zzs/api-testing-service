package com.example.apitestingservice.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VariableInterpolatorTest {

    @Test
    void replacesPlaceholders() {
        String out = VariableInterpolator.interpolate(
                "Bearer {{token}} and {{token}}",
                Map.of("token", "abc")
        );
        assertEquals("Bearer abc and abc", out);
    }

    @Test
    void leavesUnknownPlaceholdersEmpty() {
        String out = VariableInterpolator.interpolate("{{a}}-{{b}}", Map.of("a", "1"));
        assertEquals("1-", out);
    }
}
