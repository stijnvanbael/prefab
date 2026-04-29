package be.appify.prefab.processor.rest;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** Extracts path variable names (tokens in curly braces) from a URL path template. */
public final class PathVariables {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{(\\w+)}");

    private PathVariables() {}

    public static Set<String> extractFrom(String path) {
        var result = new LinkedHashSet<String>();
        var matcher = VARIABLE_PATTERN.matcher(path);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }
}



