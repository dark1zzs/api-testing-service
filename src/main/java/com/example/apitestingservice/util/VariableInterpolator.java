package com.example.apitestingservice.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces {@code {{name}}} placeholders with values from the run context map.
 */
public final class VariableInterpolator {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    private VariableInterpolator() {
    }

    public static String interpolate(String template, Map<String, String> variables) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.getOrDefault(key, "");
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
