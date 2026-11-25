import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ReleaseBundleTest {
    private static final String VERSION = "1.2.3";
    private static final String PREFIX = "be/appify/prefab/";
    private static final String ID = "01234567-89ab-cdef-0123-456789abcdef";

    private ReleaseBundleTest() {}

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("prefab-release-test-");
        Path manifest = directory.resolve("manifest.txt");
        Path bundle = directory.resolve("bundle.zip");
        try {
            Files.writeString(manifest, "prefab-parent pom\nprefab-core jar\n");
            var good = validEntries();
            Files.write(bundle, zip(good));
            byte[] validated = ReleaseBundle.validate(manifest, bundle, VERSION);
            assertTrue(Arrays.equals(validated, Files.readAllBytes(bundle)), "verified bytes differ");

            var requiredChecksums = new HashMap<>(good);
            requiredChecksums.keySet().removeIf(name -> name.endsWith(".sha256") || name.endsWith(".sha512"));
            Files.write(bundle, zip(requiredChecksums));
            ReleaseBundle.validate(manifest, bundle, VERSION);

            var windowsPaths = new HashMap<String, byte[]>();
            good.forEach((name, content) -> windowsPaths.put(name.replace('/', '\\'), content));
            Files.write(bundle, zip(windowsPaths));
            ReleaseBundle.validate(manifest, bundle, VERSION);

            var missing = new HashMap<>(good);
            missing.remove(path("prefab-core", ".jar"));
            rejects(manifest, bundle, missing, "missing=");

            var missingSources = new HashMap<>(good);
            missingSources.remove(path("prefab-core", "-sources.jar"));
            rejects(manifest, bundle, missingSources, "missing=");

            var missingJavadoc = new HashMap<>(good);
            missingJavadoc.remove(path("prefab-core", "-javadoc.jar"));
            rejects(manifest, bundle, missingJavadoc, "missing=");

            var unexpected = new HashMap<>(good);
            unexpected.put(path("showcase-example", ".pom"), bytes("example"));
            rejects(manifest, bundle, unexpected, "unexpected=");

            var classifier = new HashMap<>(good);
            classifier.put(path("prefab-core", "-tests.jar"), bytes("test"));
            rejects(manifest, bundle, classifier, "unexpected=");

            var signature = new HashMap<>(good);
            signature.remove(path("prefab-core", ".jar.asc"));
            rejects(manifest, bundle, signature, "missing=");

            var emptySignature = new HashMap<>(good);
            emptySignature.put(path("prefab-core", ".jar.asc"), new byte[0]);
            rejects(manifest, bundle, emptySignature, "Empty payload or signature");

            var checksum = new HashMap<>(good);
            checksum.put(path("prefab-core", ".jar.sha1"), bytes("bad"));
            rejects(manifest, bundle, checksum, "Invalid checksum");

            var badPath = new HashMap<>(good);
            badPath.put("../escape", bytes("bad"));
            rejects(manifest, bundle, badPath, "Invalid bundle path");

            var duplicate = new HashMap<>(good);
            duplicate.put(path("prefab-core", ".jar").replace('/', '\\'), bytes("duplicate"));
            rejects(manifest, bundle, duplicate, "Duplicate bundle entry");

            Files.write(bundle, zip(good));
            expectFailure(() -> ReleaseBundle.validate(manifest, bundle, "1.2.3-SNAPSHOT"),
                    "Invalid release version");
            Files.writeString(manifest, "prefab-core jar\nprefab-core pom\n");
            expectFailure(() -> ReleaseBundle.validate(manifest, bundle, VERSION), "duplicate manifest");
            Files.writeString(manifest, "# No approved artifacts\n");
            expectFailure(() -> ReleaseBundle.validate(manifest, bundle, VERSION), "manifest is empty");
            Files.writeString(manifest, "prefab-parent pom\nprefab-core jar tests\n");
            var withTests = new HashMap<>(good);
            addPayload(withTests, path("prefab-core", "-tests.jar"));
            Files.write(bundle, zip(withTests));
            ReleaseBundle.validate(manifest, bundle, VERSION);
            withTests.remove(path("prefab-core", "-tests.jar"));
            rejects(manifest, bundle, withTests, "missing=");
            Files.writeString(manifest, "prefab-parent pom\nprefab-core jar\n");

            Files.write(bundle, bytes("not a zip"));
            expectFailure(() -> ReleaseBundle.validate(manifest, bundle, VERSION), "zip");
            Files.write(bundle, Arrays.copyOf(validated, validated.length - 10));
            expectFailure(() -> ReleaseBundle.validate(manifest, bundle, VERSION), "zip");
            Files.delete(bundle);
            expectFailure(() -> ReleaseBundle.validate(manifest, bundle, VERSION), "bundle.zip");

            uploadUsesVerifiedBytes(validated);
            uploadReportsFailedValidation(validated);
            System.out.println("Release bundle guard tests passed");
        } finally {
            Files.deleteIfExists(bundle);
            Files.deleteIfExists(manifest);
            Files.delete(directory);
        }
    }

    private static void uploadReportsFailedValidation(byte[] bundle) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/publisher/upload", exchange -> {
            try {
                exchange.getRequestBody().readAllBytes();
                byte[] response = bytes(ID);
                exchange.sendResponseHeaders(201, response.length);
                exchange.getResponseBody().write(response);
            } finally {
                exchange.close();
            }
        });
        server.createContext("/api/v1/publisher/status", exchange -> {
            try {
                byte[] response = bytes("{\"deploymentState\":\"FAILED\",\"errors\":{\"example\":\"invalid\"}}");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            URI serverUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            expectFailure(() -> ReleasePublisher.publish(HttpClient.newHttpClient(), serverUri, "token",
                    bundle, VERSION), "failed validation");
        } finally {
            server.stop(0);
        }
    }

    private static void uploadUsesVerifiedBytes(byte[] bundle) throws Exception {
        var error = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/publisher/upload", exchange -> {
            try {
                if (!"Bearer token".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                    error.set("Incorrect authorization");
                }
                byte[] body = exchange.getRequestBody().readAllBytes();
                if (indexOf(body, bundle) < 0) {
                    error.set("Uploaded body did not contain the verified bundle");
                }
                byte[] response = bytes(ID);
                exchange.sendResponseHeaders(201, response.length);
                exchange.getResponseBody().write(response);
            } finally {
                exchange.close();
            }
        });
        server.createContext("/api/v1/publisher/status", exchange -> {
            try {
                if (!("id=" + ID).equals(exchange.getRequestURI().getQuery())) {
                    error.set("Wrong deployment ID");
                }
                byte[] response = bytes("{\"deploymentState\":\"VALIDATED\"}");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            URI serverUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            ReleasePublisher.publish(HttpClient.newHttpClient(), serverUri, "token", bundle, VERSION);
            assertTrue(error.get() == null, error.get());
        } finally {
            server.stop(0);
        }
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer: for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static Map<String, byte[]> validEntries() throws NoSuchAlgorithmException {
        var entries = new HashMap<String, byte[]>();
        addPayload(entries, path("prefab-parent", ".pom"));
        addPayload(entries, path("prefab-core", ".pom"));
        addPayload(entries, path("prefab-core", ".jar"));
        addPayload(entries, path("prefab-core", "-sources.jar"));
        addPayload(entries, path("prefab-core", "-javadoc.jar"));
        return entries;
    }

    private static void addPayload(Map<String, byte[]> entries, String name) throws NoSuchAlgorithmException {
        byte[] content = bytes("payload for " + name);
        entries.put(name, content);
        entries.put(name + ".asc", bytes("signature"));
        entries.put(name + ".md5", digest(content, "MD5"));
        entries.put(name + ".sha1", digest(content, "SHA-1"));
        entries.put(name + ".sha256", digest(content, "SHA-256"));
        entries.put(name + ".sha512", digest(content, "SHA-512"));
    }

    private static byte[] digest(byte[] content, String algorithm) throws NoSuchAlgorithmException {
        return bytes(HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(content)));
    }

    private static String path(String artifact, String suffix) {
        return PREFIX + artifact + "/" + VERSION + "/" + artifact + "-" + VERSION + suffix;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] zip(Map<String, byte[]> entries) throws IOException {
        var output = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(output)) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private static void rejects(Path manifest, Path bundle, Map<String, byte[]> entries, String message)
            throws Exception {
        Files.write(bundle, zip(entries));
        expectFailure(() -> ReleaseBundle.validate(manifest, bundle, VERSION), message);
    }

    private static void expectFailure(CheckedAction action, String message) throws Exception {
        try {
            action.run();
        } catch (IllegalArgumentException | IOException e) {
            assertTrue(e.getMessage().contains(message), "Unexpected error: " + e);
            return;
        }
        throw new AssertionError("Expected failure containing: " + message);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
