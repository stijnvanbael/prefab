---
id: TASK-399
title: Isolate benchmark agents on macOS with the Claude Code sandbox
status: To Do
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

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 When kernel isolation is unavailable, `ClaudeCodeAgent` passes sandbox and deny settings that hide the repository root (and any `--hide` paths) from the agent
- [ ] #2 The agent can still build and run its own tests in the workspace (Maven, `~/.m2`, in-memory tests), and a reference attempt still passes
- [ ] #3 An attempt that tries to read the repository gets "Operation not permitted" or a permission refusal; the run is no longer only flagged after the fact
- [ ] #4 `benchmark.md` "Isolation and known limitations" describes the macOS behaviour
<!-- AC:END -->
