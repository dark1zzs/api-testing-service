package com.example.apitestingservice.util;

import io.restassured.path.json.JsonPath;

/**
 * Shared JSONPath handling (aligned with {@link com.example.apitestingservice.tests.ApiTestExecutor}).
 */
public final class JsonPaths {

    private JsonPaths() {
    }

    public static String normalizeJsonPath(String jsonPath) {
        String trimmedPath = jsonPath.trim();

        if (trimmedPath.startsWith("$.")) {
            return trimmedPath.substring(2);
        }

        if (trimmedPath.equals("$")) {
            return "";
        }

        return trimmedPath;
    }

    /**
     * @return string form of value at path, or {@code null} if missing / invalid
     */
    public static String readString(String responseBody, String jsonPath) {
        if (responseBody == null || responseBody.isBlank() || jsonPath == null || jsonPath.isBlank()) {
            return null;
        }

        try {
            Object actualValue = JsonPath.from(responseBody).get(normalizeJsonPath(jsonPath));
            if (actualValue == null) {
                return null;
            }
            return String.valueOf(actualValue);
        } catch (Exception e) {
            return null;
        }
    }
}
