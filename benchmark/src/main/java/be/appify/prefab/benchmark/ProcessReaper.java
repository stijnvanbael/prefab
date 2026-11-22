package be.appify.prefab.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Kills processes an agent left running in its workspace. Agents start builds and servers in the background; when the
 * agent exits, these keep running, recreate Testcontainers containers with the slot's names, and remove them again when
 * they stop: in the middle of the hidden tests. Processes are found by their working directory ({@code /proc}).
 */
final class ProcessReaper {

    private ProcessReaper() {
    }

    /**
     * Kills every process whose working directory is the workspace or inside it, including its descendants.
     *
     * @param workspace the workspace
     * @return the number of processes killed
     */
    static int killProcessesIn(Path workspace) {
        var root = workspace.toAbsolutePath().normalize();
        var proc = Path.of("/proc");
        if (!Files.isDirectory(proc)) {
            return 0;
        }
        var self = ProcessHandle.current().pid();
        var victims = new ArrayList<ProcessHandle>();
        ProcessHandle.allProcesses()
                .filter(process -> process.pid() != self)
                .filter(process -> workingDirectory(proc, process.pid()).map(cwd -> cwd.startsWith(root)).orElse(false))
                .forEach(process -> {
                    victims.add(process);
                    process.descendants().forEach(victims::add);
                });
        victims.forEach(ProcessHandle::destroyForcibly);
        victims.forEach(ProcessReaper::awaitExit);
        return (int) victims.stream().map(ProcessHandle::pid).distinct().count();
    }

    private static java.util.Optional<Path> workingDirectory(Path proc, long pid) {
        try {
            return java.util.Optional.of(Files.readSymbolicLink(proc.resolve(Long.toString(pid)).resolve("cwd")));
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            return java.util.Optional.empty();
        }
    }

    private static void awaitExit(ProcessHandle process) {
        try {
            process.onExit().get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            // A process that does not exit within the grace period was already sent SIGKILL; nothing more to do.
        }
    }

    static List<Long> pidsIn(Path workspace) {
        var root = workspace.toAbsolutePath().normalize();
        return ProcessHandle.allProcesses()
                .filter(process -> workingDirectory(Path.of("/proc"), process.pid())
                        .map(cwd -> cwd.startsWith(root)).orElse(false))
                .map(ProcessHandle::pid)
                .toList();
    }
}
