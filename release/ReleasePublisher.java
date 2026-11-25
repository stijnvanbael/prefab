import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

final class ReleasePublisher {
    private static final URI CENTRAL = URI.create("https://central.sonatype.com");
    private static final Pattern DEPLOYMENT_ID = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern STATE = Pattern.compile("\"deploymentState\"\\s*:\\s*\"([A-Z]+)\"");
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration VALIDATION_TIMEOUT = Duration.ofMinutes(30);

    private ReleasePublisher() {}

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 4 || !(args[0].equals("verify") || args[0].equals("upload"))) {
            throw new IllegalArgumentException("Usage: ReleasePublisher verify|upload manifest bundle version");
        }
        byte[] bundle = ReleaseBundle.validate(Path.of(args[1]), Path.of(args[2]), args[3]);
        if (args[0].equals("upload")) {
            String username = requiredEnvironment("MAVEN_USERNAME");
            String password = requiredEnvironment("MAVEN_PASSWORD");
            String token = Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(StandardCharsets.UTF_8));
            publish(HttpClient.newHttpClient(), CENTRAL, token, bundle, args[3]);
        }
    }

    static void publish(HttpClient client, URI server, String token, byte[] bundle, String version)
            throws IOException, InterruptedException {
        String boundary = "prefab-" + UUID.randomUUID();
        byte[] prefix = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"bundle\"; filename=\"central-bundle.zip\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        var upload = HttpRequest.newBuilder(server.resolve("/api/v1/publisher/upload?name=prefab-" + version))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.concat(
                        HttpRequest.BodyPublishers.ofByteArray(prefix),
                        HttpRequest.BodyPublishers.ofByteArray(bundle),
                        HttpRequest.BodyPublishers.ofByteArray(suffix)))
                .build();
        var response = send(client, upload);
        String deploymentId = response.body().strip();
        if (!DEPLOYMENT_ID.matcher(deploymentId).matches()) {
            throw new IOException("Central returned an invalid deployment ID; inspect the portal before retrying");
        }
        System.out.println("Central deployment ID: " + deploymentId);
        waitForValidation(client, server, token, deploymentId);
    }

    private static void waitForValidation(HttpClient client, URI server, String token, String deploymentId)
            throws IOException, InterruptedException {
        long deadline = System.nanoTime() + VALIDATION_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            var request = HttpRequest.newBuilder(
                            server.resolve("/api/v1/publisher/status?id=" + deploymentId))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            var response = send(client, request);
            var match = STATE.matcher(response.body());
            if (!match.find()) {
                throw new IOException("Central returned a status without a deployment state for " + deploymentId);
            }
            String state = match.group(1);
            switch (state) {
                case "VALIDATED", "PUBLISHING", "PUBLISHED" -> {
                    System.out.println("Central deployment " + deploymentId + ": " + state);
                    return;
                }
                case "FAILED" -> throw new IOException(
                        "Central deployment " + deploymentId + " failed validation: " + response.body());
                case "PENDING", "VALIDATING" -> Thread.sleep(5_000);
                default -> throw new IOException("Unknown Central deployment state: " + state);
            }
        }
        throw new IOException("Timed out waiting for Central deployment " + deploymentId
                + "; check its state in the portal before retrying");
    }

    private static HttpResponse<String> send(HttpClient client, HttpRequest request)
            throws IOException, InterruptedException {
        var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Central request failed with HTTP " + response.statusCode()
                    + "; check the portal before retrying: " + response.body());
        }
        return response;
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable " + name);
        }
        return value;
    }
}
