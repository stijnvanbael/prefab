package be.appify.prefab.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class IsolationTest {

    @Test
    void wrapsTheCommandInAPrivateMountNamespace(@TempDir Path temporaryDirectory) {
        var repository = temporaryDirectory.resolve("repo");
        var work = temporaryDirectory.resolve("work");
        var workspace = work.resolve("run/task/project");
        var stage = temporaryDirectory.resolve("stage");
        var isolation = new Isolation.MountNamespace(List.of(repository, work));

        var command = isolation.wrap(List.of("claude", "-p", "hi"), workspace, stage);

        assertThat(command).startsWith("unshare", "--mount", "--propagation", "private", "--", "sh", "-c");
        assertThat(command.subList(8, command.size()))
                .containsExactly("isolate", workspace.toAbsolutePath().normalize().toString(),
                        stage.toAbsolutePath().normalize().toString(),
                        repository.toAbsolutePath().normalize().toString(), work.toAbsolutePath().normalize().toString(),
                        "--", "claude", "-p", "hi");
        assertThat(isolation.agentArguments(workspace, stage)).isEmpty();
    }

    @Test
    void leavesTheCommandAloneWhenDisabled(@TempDir Path temporaryDirectory) {
        var workspace = temporaryDirectory.resolve("workspace");
        var stage = temporaryDirectory.resolve("stage");
        assertThat(Isolation.none().wrap(List.of("claude"), workspace, stage)).containsExactly("claude");
        assertThat(Isolation.none().agentArguments(workspace, stage)).isEmpty();
        assertThat(Isolation.none().enabled()).isFalse();
    }

    @Nested
    class ClaudeSandbox {
        private static final JsonMapper JSON = JsonMapper.builder().build();

        @TempDir
        Path root;
        private Path repository;
        private Path workRoot;
        private Path workspace;
        private Path otherRun;
        private Path agentTemp;
        private Path otherAgentTemp;

        @BeforeEach
        void createLayout() throws IOException {
            root = root.toRealPath();
            repository = Files.createDirectories(root.resolve("sources/prefab"));
            workRoot = Files.createDirectories(root.resolve("work"));
            workspace = Files.createDirectories(workRoot.resolve("run-2/rest-crud-1/project"));
            Files.createDirectories(workRoot.resolve("run-2/rest-crud-1/logs"));
            otherRun = Files.createDirectories(workRoot.resolve("run-1"));
            agentTemp = Files.createDirectories(root.resolve("agent/attempt-2"));
            otherAgentTemp = Files.createDirectories(root.resolve("agent/attempt-1"));
        }

        @Test
        void deniesShellReadsOfHiddenPathsButKeepsTheWorkspaceAndAgentTempReadable() {
            var settings = settings(List.of(repository, workRoot));

            var filesystem = settings.at("/sandbox/filesystem");
            assertThat(strings(filesystem.get("denyRead")))
                    .containsExactly(repository.toString(), workRoot.toString(), agentTemp.getParent().toString());
            assertThat(strings(filesystem.get("allowRead")))
                    .containsExactly(workspace.toString(), agentTemp.toString());
            assertThat(settings.at("/sandbox/enabled").asBoolean()).isTrue();
            assertThat(settings.at("/sandbox/allowUnsandboxedCommands").asBoolean()).isFalse();
        }

        @Test
        void keepsDockerReachable() {
            var network = settings(List.of(repository)).at("/sandbox/network");

            assertThat(strings(network.get("allowUnixSockets"))).contains("/var/run/docker.sock");
            assertThat(network.get("allowLocalBinding").asBoolean()).isTrue();
        }

        @Test
        void deniesFileToolsEverythingInAHiddenFolderExceptThePathToTheWorkspace() {
            var rules = strings(settings(List.of(repository, workRoot)).at("/permissions/deny"));

            assertThat(rules).contains(
                    rule("Read", repository), rule("Edit", repository),
                    rule("Read", otherRun), rule("Read", workRoot.resolve("run-2/rest-crud-1/logs")),
                    rule("Read", otherAgentTemp));
            assertThat(rules).noneMatch(rule -> rule.contains("/project") || rule.contains("attempt-2")
                    || rule.equals(rule("Read", workRoot)) || rule.equals(rule("Read", workRoot.resolve("run-2"))));
        }

        @Test
        void deniesBothSpellingsOfLinkedSystemFolders() throws IOException {
            var hidden = Files.createDirectory(root.resolve("linked-system-folder"));
            var realPath = hidden.toRealPath().toString();
            if (!realPath.startsWith("/private/")) {
                return;
            }
            var linkedRoot = List.of("/var/", "/tmp/", "/etc/").stream()
                    .filter(root -> realPath.startsWith("/private" + root))
                    .findFirst();
            if (linkedRoot.isEmpty()) {
                return;
            }

            var rules = strings(settings(List.of(hidden)).at("/permissions/deny"));
            var alternatePath = realPath.substring("/private".length());

            assertThat(rules).contains("Read(/" + realPath + "/**)", "Read(/" + alternatePath + "/**)");
        }

        @Test
        void passesTheSettingsAsOneCliArgument() {
            var arguments = new Isolation.ClaudeSandbox(List.of(repository)).agentArguments(workspace, agentTemp);

            assertThat(arguments).hasSize(2).first().isEqualTo("--settings");
            assertThat(JSON.readTree(arguments.get(1)).at("/sandbox/filesystem/denyRead").isArray()).isTrue();
        }

        private JsonNode settings(List<Path> hidden) {
            Map<String, Object> settings = new Isolation.ClaudeSandbox(hidden).settings(workspace, agentTemp);
            return JSON.valueToTree(settings);
        }

        private static String rule(String tool, Path path) {
            return tool + "(/" + path + "/**)";
        }

        private static List<String> strings(JsonNode array) {
            return array.valueStream().map(JsonNode::asString).toList();
        }
    }
}
