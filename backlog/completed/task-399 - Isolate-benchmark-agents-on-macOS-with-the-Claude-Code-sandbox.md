---
id: TASK-399
title: Isolate benchmark agents on macOS with the Claude Code sandbox
status: Done
assignee: []
created_date: '2026-11-26 13:15'
labels:
  - agents
  - testing
dependencies: []
references:
  - benchmark/src/main/java/be/appify/prefab/benchmark/ClaudeCodeAgent.java
  - backlog/docs/benchmark.md
priority: high
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found in TASK-392. Repository hiding needs Linux, root and `unshare`; on macOS the harness only warns. A
`qwen38-local` attempt on `rest-parent-child` ran `find / -name ...`, found the checkout and read
`benchmark/tasks/rest-parent-child/reference/.../Task.java`, the reference solution, plus processor test sources.
The run was stopped and discarded. Earlier Qwen3-Coder attempts also wrote files under `~/.claude/projects`.

Verified by hand (Claude Code 2.1.332, macOS): passing
`--settings '{"sandbox":{"enabled":true,"autoAllowBashIfSandboxed":true,"allowUnsandboxedCommands":false,"filesystem":{"denyRead":["<repo root>"]}},"permissions":{"deny":["Read(//<repo root>/**)","Edit(//<repo root>/**)"]}}'`
makes a Bash read of the repo fail with "Operation not permitted" and the Read tool refuse it, while
`mvn clean verify` in a workspace under the system temp directory still succeeds and `~/.m2` stays readable. Note
that permission rules need the `//` prefix for absolute paths; `Read(/Users/...)` did not apply.

Hiding only the checkout is not enough: with the repo root denied, a Qwen 3.8 attempt went up to
`~/IdeaProjects/appify/` and read another application built with Prefab (`energy-balancer`), which is just as much a
leak. The TASK-392 baseline therefore denies all of `~/IdeaProjects`. The harness should deny the user's source
folders broadly (configurable), not just the repository root.

Side effect seen in the TASK-392 baseline: the sandbox also blocks the Docker socket, so the agent's own tests can
only use Prefab's in-memory backend (an agent noticed and switched). Hidden tests run outside the sandbox and are
unaffected, but hosted runs without the sandbox can use Testcontainers. This is not harmless: an agent spent over an hour on one quick task investigating why the prompt's "Docker is
available" did not hold. Verified fix: `"network": {"allowUnixSockets": ["~/.docker/run/docker.sock",
"/var/run/docker.sock"], "allowLocalBinding": true}` makes `docker ps` and localhost connections work inside the
sandbox while repository reads stay blocked.

More leak sources found in the TASK-392 baseline, each read by an agent that passed the hidden tests:
- another full checkout in `/private/tmp/prefab-310.*` (left by a different session), including `benchmark/tasks/*/reference`;
- earlier runs' kept workspaces under `$TMPDIR/prefab-benchmark/`, among them `validate-*-reference`, which holds the
  reference solution of every task.
`/tmp` itself cannot be denied, because Claude Code's own temp directories (`prefab-benchmark-agent-tmp-*`) live
there. The harness should give each run a fresh work directory, deny the shared work root and other runs' agent
temp directories, deny both `/var` and `/private/var` (and `/tmp` and `/private/tmp`) spellings in permission rules,
and ideally remove `validate` workspaces after validation.
<!-- SECTION:DESCRIPTION:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Analysis (2026-11-27, Claude Code 2.1.332, macOS), verified by hand with `claude -p` and a Bash probe:
- `sandbox.filesystem.allowRead` re-allows a path inside a `denyRead` path. A whole folder (the shared work root, the
  user's source folder) can therefore be hidden while the attempt's own workspace stays visible, so the workspace does
  not have to move.
- Hiding all of `/tmp` breaks the agent: its shell cannot write its cwd file in its own temp directory even when
  re-allowed. Instead, every attempt's Claude Code temp directory goes under one root (`/tmp/prefab-benchmark-agent`),
  which is hidden with only the attempt's own directory re-allowed (real `/private/tmp` paths).
- Maven Central (HTTPS), `~/.m2` writes and the Docker socket work inside the sandbox with
  `network.allowUnixSockets` and `allowLocalBinding`.
- Permission deny rules (Read/Edit/Grep/Glob tools) have no re-allow. For a hidden folder that contains an allowed
  path, the harness denies each of its children except the ones leading to the allowed path (snapshot at start; the
  sandbox still covers what appears later). Rules are written for both `/private/var` and `/var` (and `/tmp`)
  spellings.

Design:
1. `Isolation` becomes a sealed interface: `MountNamespace` (Linux root + unshare, unchanged), `ClaudeSandbox`
   (macOS: `--settings` with sandbox + permission deny rules) and `None`.
2. Hidden by default: the repository, the folder that holds the checkout (sibling projects such as other Prefab apps;
   skipped when it is the home directory or `/`), the shared work root, `--hide` paths, and for the sandbox the
   agent-temp root.
3. A profile that passes its own `--settings` conflicts with the sandbox settings; the run fails with a clear message.
<!-- SECTION:PLAN:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 When kernel isolation is unavailable, `ClaudeCodeAgent` passes sandbox and deny settings that hide the repository root (and any `--hide` paths) from the agent
- [x] #2 The agent can still build and run its own tests in the workspace (Maven, `~/.m2`, in-memory tests), and a reference attempt still passes
- [x] #3 An attempt that tries to read the repository gets "Operation not permitted" or a permission refusal; the run is no longer only flagged after the fact
- [x] #4 `benchmark.md` "Isolation and known limitations" describes the macOS behaviour
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- `Isolation` is a sealed interface: `MountNamespace` (Linux root, unchanged), `ClaudeSandbox` (macOS) and `None`.
  `ClaudeSandbox.agentArguments` builds one `--settings` JSON: sandbox enabled, no unsandboxed fallback, `denyRead`
  for the hidden paths plus the agent-temp root, `allowRead` for the workspace and the attempt's temp directory, the
  Docker sockets and local binding allowed, and `Read`/`Edit` deny rules (`//path` and `//path/**`, both
  `/private/var` and `/var` spellings). For a hidden folder that holds the workspace, every other entry is denied as
  found at the start of the attempt.
- `ClaudeCodeAgent` puts each attempt's temp directory under `/tmp/prefab-benchmark-agent`, adds the isolation
  arguments, keeps them in the attempt's `isolation-arguments.txt`, and refuses a profile that passes its own
  `--settings` when the sandbox is active.
- `BenchmarkCli` also hides the folder that holds the checkout (other applications next to it), unless that is the
  home directory or `/`, and prints what is hidden and how.
- Verified on macOS (Claude Code 2.1.332): a Haiku attempt on `rest-create-and-get` with `cheat-sheet` passed in about
  1 minute, running `mvn compile` and `mvn test` (3 tests) in the workspace. A probe with the generated settings got
  `Operation not permitted` for `head` on a reference solution and `ls` on the source folder, and "File is in a
  directory that is denied by your permission settings" from the Read tool.
- Not done: removing `validate` workspaces. The work root that holds them is now hidden, so they no longer leak.
  Hiding all of `/tmp` is not possible (the agent's own shell breaks), so a stray checkout there needs `--hide`; the
  transcript scan still flags `benchmark/tasks/` reads.
- The local, git-excluded `benchmark/models/qwen38-sandboxed.yml` stand-in is now obsolete; with its `--settings` the
  run refuses to start. Use `qwen38-local`.
<!-- SECTION:NOTES:END -->
