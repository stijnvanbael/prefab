package be.appify.prefab.benchmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parsed command line: a command, {@code --option value} pairs, {@code --flag}s and positional arguments.
 *
 * @param command    the first argument
 * @param options    option values by name (without dashes)
 * @param flags      flags that were given
 * @param positional remaining arguments
 */
record CliArguments(String command, Map<String, String> options, Set<String> flags, List<String> positional) {
    private static final Set<String> FLAGS = Set.of("keep-workspaces", "help");

    CliArguments {
        options = Map.copyOf(options);
        flags = Set.copyOf(flags);
        positional = List.copyOf(positional);
    }

    static CliArguments parse(String... args) {
        if (args.length == 0) {
            return new CliArguments("help", Map.of(), Set.of(), List.of());
        }
        var options = new HashMap<String, String>();
        var flags = new HashSet<String>();
        var positional = new ArrayList<String>();
        for (int i = 1; i < args.length; i++) {
            var arg = args[i];
            if (!arg.startsWith("--")) {
                positional.add(arg);
                continue;
            }
            var name = arg.substring(2);
            var equals = name.indexOf('=');
            if (equals >= 0) {
                options.put(name.substring(0, equals), name.substring(equals + 1));
            } else if (FLAGS.contains(name)) {
                flags.add(name);
            } else if (i + 1 < args.length) {
                options.put(name, args[++i]);
            } else {
                throw new IllegalArgumentException("Option --" + name + " needs a value");
            }
        }
        return new CliArguments(args[0], options, flags, positional);
    }

    Optional<String> option(String name) {
        return Optional.ofNullable(options.get(name));
    }

    String option(String name, String defaultValue) {
        return options.getOrDefault(name, defaultValue);
    }

    int intOption(String name, int defaultValue) {
        var value = options.get(name);
        if (value == null) {
            return defaultValue;
        }
        var parsed = Integer.parseInt(value);
        if (parsed < 1) {
            throw new IllegalArgumentException("--" + name + " must be at least 1");
        }
        return parsed;
    }

    List<String> listOption(String name) {
        var value = options.get(name);
        return value == null ? List.of() : Arrays.stream(value.split(",")).map(String::strip)
                .filter(item -> !item.isEmpty()).toList();
    }

    boolean flag(String name) {
        return flags.contains(name);
    }
}
