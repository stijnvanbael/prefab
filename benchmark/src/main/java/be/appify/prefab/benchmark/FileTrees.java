package be.appify.prefab.benchmark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** File-tree helpers: overlaying directories onto a workspace and deleting trees. */
final class FileTrees {

    private FileTrees() {
    }

    /**
     * Copies a file or directory tree onto a target, replacing existing files with the same relative path.
     *
     * @param source file or directory
     * @param target file or directory to create or overlay
     */
    static void copy(Path source, Path target) {
        if (!Files.exists(source)) {
            throw new IllegalArgumentException(source + " does not exist");
        }
        try {
            if (Files.isRegularFile(source)) {
                Files.createDirectories(target.toAbsolutePath().getParent());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                return;
            }
            for (var file : files(source)) {
                var destination = target.resolve(source.relativize(file).toString());
                Files.createDirectories(destination.getParent());
                Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot copy " + source + " to " + target, e);
        }
    }

    /** Regular files below a directory, sorted, or an empty list when the directory does not exist. */
    static List<Path> files(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Deletes a directory tree if it exists. */
    static void delete(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (var path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete " + directory, e);
        }
    }

    static String readString(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    static String readStringIfExists(Path file) {
        return Files.isRegularFile(file) ? readString(file) : "";
    }

    static void append(Path file, String content) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, content, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + file, e);
        }
    }

    /**
     * Shrinks a file that exceeds a size by keeping its first and last {@code maxBytes / 2} bytes.
     *
     * @param file     the file
     * @param maxBytes maximum size to keep
     */
    static void truncateMiddle(Path file, long maxBytes) {
        try {
            if (!Files.isRegularFile(file) || Files.size(file) <= maxBytes) {
                return;
            }
            var half = (int) (maxBytes / 2);
            var size = Files.size(file);
            var head = new byte[half];
            var tail = new byte[half];
            try (var channel = java.nio.channels.FileChannel.open(file, java.nio.file.StandardOpenOption.READ)) {
                channel.read(java.nio.ByteBuffer.wrap(head), 0);
                channel.read(java.nio.ByteBuffer.wrap(tail), size - half);
            }
            var marker = "\n\n... [%d bytes removed by the benchmark harness] ...\n\n".formatted(size - 2L * half)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            try (var out = Files.newOutputStream(file)) {
                out.write(head);
                out.write(marker);
                out.write(tail);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot truncate " + file, e);
        }
    }

    /** Streams the lines of a file through a function, tolerating malformed UTF-8, or returns the empty result. */
    static <T> T withLines(Path file, java.util.function.Function<Stream<String>, T> function, T empty) {
        if (!Files.isRegularFile(file)) {
            return empty;
        }
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(Files.newInputStream(file),
                java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE)))) {
            return function.apply(reader.lines());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    static void writeString(Path file, String content) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + file, e);
        }
    }
}
