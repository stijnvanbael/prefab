import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.CRC32;
import java.util.zip.ZipFile;

final class ReleaseBundle {
    private static final String GROUP_PATH = "be/appify/prefab/";
    private static final Pattern NAME = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Pattern VERSION = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+");
    private static final Map<String, String> CHECKSUMS = Map.of(
            ".md5", "MD5", ".sha1", "SHA-1", ".sha256", "SHA-256", ".sha512", "SHA-512");

    private ReleaseBundle() {}

    static byte[] validate(Path manifest, Path bundle, String version) throws IOException {
        if (!VERSION.matcher(version).matches()) {
            throw new IllegalArgumentException("Invalid release version: " + version);
        }
        var payloads = expectedPayloads(manifest, version);
        byte[] bytes = Files.readAllBytes(bundle);
        var entries = readEntries(bytes);
        var allowed = new HashSet<String>();
        var required = new HashSet<String>();
        for (String payload : payloads) {
            required.add(payload);
            required.add(payload + ".asc");
            required.add(payload + ".md5");
            required.add(payload + ".sha1");
            allowed.addAll(required);
            allowed.add(payload + ".sha256");
            allowed.add(payload + ".sha512");
        }
        var missing = new HashSet<>(required);
        missing.removeAll(entries.keySet());
        var unexpected = new HashSet<>(entries.keySet());
        unexpected.removeAll(allowed);
        if (!missing.isEmpty() || !unexpected.isEmpty()) {
            throw new IllegalArgumentException("Bundle mismatch: missing=" + sorted(missing)
                    + ", unexpected=" + sorted(unexpected));
        }
        for (String payload : payloads) {
            if (entries.get(payload).length == 0 || entries.get(payload + ".asc").length == 0) {
                throw new IllegalArgumentException("Empty payload or signature: " + payload);
            }
            for (var checksum : CHECKSUMS.entrySet()) {
                String path = payload + checksum.getKey();
                if (entries.containsKey(path)) {
                    verifyChecksum(entries.get(payload), entries.get(path), checksum.getValue(), path);
                }
            }
        }
        System.out.printf("Verified %d files for %d artifacts in %s%n",
                entries.size(), payloads.size(), bundle);
        return bytes;
    }

    private static Set<String> expectedPayloads(Path manifest, String version) throws IOException {
        var payloads = new HashSet<String>();
        var coordinates = new HashSet<String>();
        for (String line : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] fields = line.split("\\s+");
            if (fields.length < 2 || fields.length > 3 || !NAME.matcher(fields[0]).matches()
                    || !(fields[1].equals("pom") || fields[1].equals("jar"))
                    || (fields.length == 3 && (!fields[1].equals("jar") || !NAME.matcher(fields[2]).matches()))
                    || !coordinates.add(fields[0])) {
                throw new IllegalArgumentException("Invalid or duplicate manifest entry: " + line);
            }
            String base = GROUP_PATH + fields[0] + "/" + version + "/" + fields[0] + "-" + version;
            payloads.add(base + ".pom");
            if (fields[1].equals("jar")) {
                payloads.add(base + ".jar");
                payloads.add(base + "-sources.jar");
                payloads.add(base + "-javadoc.jar");
                if (fields.length == 3) {
                    payloads.add(base + "-" + fields[2] + ".jar");
                }
            }
        }
        if (payloads.isEmpty()) {
            throw new IllegalArgumentException("Artifact manifest is empty");
        }
        return payloads;
    }

    private static Map<String, byte[]> readEntries(byte[] bytes) throws IOException {
        var entries = new HashMap<String, byte[]>();
        Path temporary = Files.createTempFile("prefab-central-bundle-", ".zip");
        try {
            Files.write(temporary, bytes);
            try (var zip = new ZipFile(temporary.toFile())) {
                var iterator = zip.entries();
                while (iterator.hasMoreElements()) {
                    ZipEntry entry = iterator.nextElement();
                    String name = entry.getName().replace('\\', '/');
                    if (name.startsWith("/") || name.contains("//") || name.contains("../")
                            || name.contains("./") || name.isEmpty()) {
                        throw new IllegalArgumentException("Invalid bundle path: " + name);
                    }
                    if (entry.isDirectory()) {
                        continue;
                    }
                    byte[] content;
                    try (var stream = zip.getInputStream(entry)) {
                        content = stream.readAllBytes();
                    }
                    var crc = new CRC32();
                    crc.update(content);
                    if (crc.getValue() != entry.getCrc()) {
                        throw new IllegalArgumentException("Invalid ZIP checksum: " + name);
                    }
                    if (entries.putIfAbsent(name, content) != null) {
                        throw new IllegalArgumentException("Duplicate bundle entry: " + name);
                    }
                }
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Bundle is empty");
        }
        return entries;
    }

    private static void verifyChecksum(byte[] payload, byte[] value, String algorithm, String path) {
        try {
            String expected = HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(payload));
            String actual = new String(value, StandardCharsets.US_ASCII).strip();
            if (!expected.equalsIgnoreCase(actual)) {
                throw new IllegalArgumentException("Invalid checksum: " + path);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing checksum algorithm: " + algorithm, e);
        }
    }

    private static List<String> sorted(Set<String> values) {
        var result = new ArrayList<>(values);
        result.sort(String::compareTo);
        return result;
    }
}
