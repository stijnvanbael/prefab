# Agent Benchmark

**For Prefab contributors.** This guide is not part of the application developer documentation.

The benchmark measures how well an AI coding agent builds Prefab applications. Each task gives the agent a starting
project and a natural-language prompt. Hidden acceptance tests, which the agent never sees, then decide whether the
result works. Run the same tasks with different models and documentation setups and you can see whether a doc change,
a better diagnostic or a new recipe actually helps, rather than guessing.

The question it answers: *can a small, cheap model such as Claude Haiku develop Prefab applications flawlessly, and
what stops it?* Every failure is classified, so each one points to a follow-up: a doc gap, a misleading diagnostic,
an API that is easy to misuse, or a place where the model fell back to plain Spring code.

Tasks come in three [tiers](#tiers): quick single-feature tasks, feature-sized user stories and brownfield changes to
an existing application. Reports show the pass rate per tier.

---

## Contents

- [Requirements](#requirements)
- [Running the benchmark](#running-the-benchmark)
- [Tiers](#tiers)
- [How an attempt works](#how-an-attempt-works)
- [Context setups](#context-setups)
- [Reading the report](#reading-the-report)
- [Classifying failures](#classifying-failures)
- [Adding a task](#adding-a-task)
- [Adding a brownfield task](#adding-a-brownfield-task)
- [Running on a schedule](#running-on-a-schedule)
- [Reproducibility](#reproducibility)
- [Isolation and known limitations](#isolation-and-known-limitations)
- [Recorded baselines](#recorded-baselines)

---

## Requirements

- JDK and Maven, as for building Prefab itself.
- Docker. Hidden tests run Postgres, Kafka, the schema registry and MongoDB with Testcontainers. **No cloud
  credentials are needed**: nothing talks to AWS, GCP or a real identity provider.
- The Claude Code CLI (`claude`), logged in or with `ANTHROPIC_API_KEY` set. Only `run` needs it; `validate`,
  `list`, `compare` and `classify` work without it.
- **Usage budget.** The quick tier alone is 33 agent sessions; the feature and brownfield tiers add 14 longer ones. On
  a Claude subscription, two Haiku runs in parallel used up a 5-hour usage limit within minutes, so prefer an API key
  for baselines, run one setup at a time, or pick tasks with `--tasks`. When the API answers with a usage or rate
  limit (HTTP 429), the harness stops starting new attempts, marks the remaining ones as skipped, flags the report as
  not meaningful and exits with code 3. After the limit resets, continue with `benchmark/run.sh run --resume
  benchmark/results/<run-id>`.

## Running the benchmark

All commands go through `benchmark/run.sh`, run from the repository root:

```bash
benchmark/run.sh list
```

```bash
benchmark/run.sh validate --parallel 3
```

```bash
benchmark/run.sh run --model claude-haiku-5-5 --context full-docs --parallel 3
```

```bash
benchmark/run.sh run --model claude-haiku-5-5 --context none --tasks 'rest-*,events-handling' --repeat 3
```

```bash
benchmark/run.sh run --model claude-haiku-5-5 --context cheat-sheet --tasks feature,brownfield --repeat 3
```

```bash
benchmark/run.sh compare benchmark/results/*/report.json --output /tmp/comparison.md
```

`run` and `validate` first install the working-tree Prefab into the local Maven repository (without the example
modules; see [Isolation](#isolation-and-known-limitations)), so the benchmark always measures the code you have
checked out. Set `BENCHMARK_SKIP_INSTALL=1` to skip that step when nothing changed.

| Option | Default | Meaning |
|---|---|---|
| `--model` | (required unless `--profile`) | Model to test, passed to `claude --model`, e.g. `claude-haiku-5-5`, `claude-sonnet-5-5`. |
| `--profile` | none | Model profile from `benchmark/models/` (model, endpoint, timeouts); see [Model profiles](#model-profiles). `--model` overrides the profile's model. |
| `--context` | `none` | Documentation setup from `benchmark/contexts/`. |
| `--agent` | `claude` | `claude`, or the stand-ins `reference` (applies the reference solution) and `noop` (changes nothing). |
| `--tasks` | all | Comma-separated task ids, categories or tiers (`quick`, `feature`, `brownfield`); globs such as `events-*` work. |
| `--repeat` | 1 | Attempts per task. Use 3 or more when comparing setups (see [Reproducibility](#reproducibility)). |
| `--parallel` | 2 (or the profile's) | Attempts running at the same time. Each needs about 2 CPU cores and 3 GB of memory. |
| `--triage-model` | none | Let this model classify failures (see [Classifying failures](#classifying-failures)). |
| `--results` | `benchmark/results/<run-id>` | Where the report and logs go. |
| `--work-dir` | `<tmp>/prefab-benchmark/<run-id>` | Where the workspaces go. Keep this outside the repository. |
| `--hide` | none | Extra comma-separated paths hidden from the agent, on top of the repository, the folder that holds it and the work root (see [Isolation](#isolation-and-known-limitations)). |
| `--resume <run-dir>` | none | Continue a run that a usage limit stopped: rerun its rate-limited and skipped attempts with the same model and context, and merge them into its report. |
| `--keep-workspaces` | off | Keep the workspaces of passed attempts. Failed ones are always kept. |

### Model profiles

The agent is always Claude Code, but the model behind it is pluggable. A profile in `benchmark/models/<name>.yml`
names the model and, when it is not served by the hosted API, where it is served. `benchmark/run.sh list` shows the
profiles. Without `--profile`, `--model <id>` works as before.

| Field | Default | Meaning |
|---|---|---|
| `model` | (required) | Passed to `claude --model`. |
| `env` | none | Environment variables for the agent process only, for example the endpoint. Maven and acceptance runs do not see them. |
| `args` | none | Extra arguments for the Claude Code CLI. |
| `timeoutFactor` | 1 | Multiplies each task's timeout, for models slower than the hosted default. |
| `parallel` | 2 | Default for `--parallel`. |

The baseline model is Qwen 3.8 27B served by Ollama (`qwen38-local`), so documentation setups can be compared
without spending API tokens. `qwen-local` (Qwen3-Coder 30B) is kept as a weaker local model: it failed every quick
task it tried, so it does not separate setups (see [Recorded baselines](#recorded-baselines)). Haiku and Sonnet
profiles are shipped for the hosted runs. To use another model, copy a profile and change `model` and `env`.

```bash
OLLAMA_CONTEXT_LENGTH=131072 ollama serve
```

With the macOS app, set the variable with `launchctl setenv OLLAMA_CONTEXT_LENGTH 131072`, then quit and reopen the
app; `ollama ps` shows the context each loaded model got.

```bash
benchmark/run.sh run --profile qwen38-local --context cheat-sheet --tasks feature --repeat 3
```

Things to know about a local model:

- Ollama must run with a context window of at least 32k tokens; use 128k. Its default is too small for Claude Code's
  system prompt, and attempts then fail in confusing ways. `full-docs` (about 60k tokens) only fits in a 128k window.
  On 48 GB of unified memory both Qwen models stay fully on the GPU at 128k.
- Claude Code does not know the context window of a custom model id and assumes 1M tokens, so it will not compact a
  long session before Ollama truncates it. Keep that in mind for the long feature and brownfield tasks.
- Keep the agent's own prompt small. Claude Code's full tool set and its auto-memory section take about 12k tokens
  before any task or docs. Both Qwen profiles therefore pass `--tools Bash,Read,Edit,Write` and set
  `CLAUDE_CODE_DISABLE_AUTO_MEMORY=1`, which brings the starting prompt to about 3.4k tokens. Without the latter,
  Qwen took the memory directory (`~/.claude/projects/<mangled path>/memory`) for its working directory and wrote
  its code there (TASK-392). Copy both settings into any profile for a small model.
- Pick a model with reliable tool calling. A model that cannot drive the tools scores 0 and shows nothing about
  the documentation.
- The cost column shows what Claude Code would charge at API prices, which means nothing for a local model; compare
  turns, builds, agent time and pass rate (TASK-397 replaces it with an energy-based cost estimate).
- Local agents do search the disk: in TASK-392 they read reference solutions in the checkout, in another checkout
  under `/tmp` and in kept workspaces of earlier runs, and code of another Prefab application. On macOS the harness
  hides these through Claude Code's sandbox (see [Isolation](#isolation-and-known-limitations)); pass `--hide` for
  source folders or checkouts elsewhere. A profile must not pass its own `--settings`: they would replace the
  sandbox settings, so the run refuses to start.
- Do not compare pass rates across models. Compare documentation setups on the same model. A result on a weaker model
  shows whether a setup helps; it does not predict Haiku's absolute pass rate.
- Check the Anthropic-compatible endpoint of your Ollama version with a single task before starting a long run:
  `benchmark/run.sh run --profile qwen38-local --context none --tasks <one task id>`.

`validate` runs every task twice, with stand-in agents. The reference solution must pass and the unchanged starting
project must fail. Run it after changing a task or Prefab itself. A task that no longer validates measures nothing.

## Tiers

The 33 quick tasks are one feature on a tiny starting project each, done in a minute or two. Every documentation setup
passes almost all of them (see [Recorded baselines](#recorded-baselines)), so they no longer show whether a doc change
helps. Real work is bigger: a feature across several aggregates from a product-style spec, or a change to code the
agent did not write. That is where small models drift, fall back to plain Spring or work around generated code.
`tier` in `task.yml` says which kind a task is:

| Tier | Tasks | What the agent gets | What it measures |
|---|---|---|---|
| `quick` (default) | 33 | A tiny starter and a prompt that names the contract | Whether the model knows the Prefab way for one intent; regressions |
| `feature` | 8 | An empty project and a user story with acceptance criteria, in product language | Designing several aggregates, events and handlers, often with security or tenancy |
| `brownfield` | 6 | The `shop` application (`benchmark/apps/shop`) plus a starter, and a change request or bug report | Reading existing code, then adding a feature, changing a contract, evolving a schema that holds data, fixing a failing test or a reported bug |

`run` without `--tasks` runs every tier. Use `--tasks quick` for the regression run (it is what the
[schedule](#running-on-a-schedule) runs) and `--tasks feature,brownfield` to compare documentation setups. Feature and
brownfield tasks take longer: they allow 40 to 45 minutes and 150 turns.

## How an attempt works

1. **Workspace.** The harness copies `benchmark/template` (a standalone Spring Boot + Prefab Maven project), overlays
   the task's base application (`benchmark/apps/<base>/project`, brownfield tasks only) and then the task's
   `starter/`, applies the context setup, and commits the result to a fresh git repository outside the Prefab
   checkout.
2. **Agent.** Claude Code runs headless in the workspace:
   `claude -p <preamble + prompt> --model <model> --output-format stream-json --permission-mode bypassPermissions
   --setting-sources project --disallowedTools WebFetch,WebSearch --max-turns <n>`. Only the workspace's own
   CLAUDE.md is loaded; your personal settings and memory are not. The preamble (`benchmark/agent-preamble.md`)
   tells the agent to work autonomously and that Maven and Docker are available.
3. **Acceptance.** When the agent stops (it says it is done, runs out of turns, or hits the task timeout):
   - **guardrails** scan the files the agent changed for plain Spring habits. Hand-written controllers, JPA,
     Spring Data repositories, JdbcTemplate/MongoTemplate, Lombok and source files that replace generated classes
     fail the attempt. A new SQL migration is reported as a warning.
   - for test-writing tasks, the agent's own tests must pass, `src/main` must be unchanged, and the tests must fail
     on every **mutant** (a copy of the code with a deliberate bug).
   - the **hidden tests** are copied in and run with Maven. They talk to the application over HTTP (MockMvc), so
     they depend only on the API contract in the prompt, not on how the agent named Java methods.
4. **Report.** The transcript, the agent's diff and all build logs are kept per attempt under
   `benchmark/results/<run-id>/attempts/` (not committed). `report.json` and `report.md` summarise the run.

Before the agent starts and again before acceptance, the harness kills any process the agent left running in its
workspace and removes the slot's Testcontainers containers. Container reuse is disabled
(`TESTCONTAINERS_REUSE_ENABLE=false`): Testcontainers matches reusable containers by a configuration hash that ignores
the container name, so parallel slots would otherwise share, and remove, each other's databases (TASK-356). Every
attempt starts with an empty database and an empty Kafka broker, and the hidden tests never see messages left behind
by the agent's own test runs.

## Context setups

A context setup is the documentation the agent works with. It lives in `benchmark/contexts/<name>/context.yml`:

```yaml
description: One line shown by `list` and in reports.
instructionsFile: CLAUDE.md   # default
instructions:                  # concatenated into the instructions file, in order
  - context: CLAUDE.md         # a file in this context directory
  - repo: backlog/docs/*.md    # a file or glob in the Prefab repository
copy:                          # repository files or directories copied into the workspace
  - from: backlog/docs/feature-guides.md
    to: docs/prefab/feature-guides.md
```

| Context | What the agent gets |
|---|---|
| `none` | Nothing beyond the project and the prompt. It can still read the Prefab jars in `~/.m2`, as any user can. |
| `full-docs` | The readme and all developer guide chapters (about 250 KB, 60k tokens) inside CLAUDE.md, always loaded. |
| `docs-folder` | The same docs copied to `docs/prefab/`, with a short CLAUDE.md that points to them. Read on demand. |
| `cheat-sheet` | Only the [agent cheat sheet](agent-cheat-sheet.md) (about 2k tokens) in CLAUDE.md, always loaded. |
| `recipes` | Cheat sheet plus the recipe index in CLAUDE.md; recipes and their verified source examples are copied and opened on demand. |
| `local-run` | Cheat sheet plus an explicit instruction to start the in-memory application and verify an affected endpoint over HTTP. |

**`recipes` is the baseline context** (decision-014): measure new aids and documentation changes against it, not
against `cheat-sheet` or `full-docs`. It was the fastest and cheapest setup on the local Qwen 3.8 baseline with the
same pass rate as the others (see [Recorded baselines](#recorded-baselines)); that lead is not yet statistically
confirmed. Record `none` once per model as a reference. The Haiku `recipes` run follows in TASK-389; do not treat the
earlier Haiku cheat-sheet or full-docs baselines as results for this context. The `--context` default stays `none`, so
pass `--context recipes` explicitly.

The compact agent cheat sheet (TASK-344) has the `cheat-sheet` context. The `recipes` context makes the TASK-346
recipe index available in the always-loaded instructions and copies recipe files to `docs/prefab/recipes/`, with
their linked showcase and processor-test sources at repository-relative paths for on-demand reading. Once
TASK-350 delivers agent guidance to application projects, the benchmark must use that same delivery path. Then add a
context that reproduces the setup step, so the benchmark measures what users get.

## Reading the report

`report.md` starts with a summary:

- **Pass rate** with a 95% Wilson confidence interval. With 33 tasks and one attempt each, the interval is about
  ±15 percentage points wide, so small differences between runs are noise. See [Reproducibility](#reproducibility).
- **Tasks passed in every attempt**: the stable successes when `--repeat` is above 1.
- **Cost**, **tokens** (input including prompt-cache reads and writes, and output), and the median **turns**,
  **builds** (Maven invocations by the agent, a proxy for compile-and-fix retries) and **agent time**
  (wall-clock time).
- **Median time to first green build**: from the start of the agent until the first Maven command it ran that
  succeeded, over the attempts that had one ("40s (28/32 attempts)"). A command counts as green when Claude Code does
  not report it as an error and its output shows no build failure, compilation error or failing test. Claude Code's
  stream has no timestamps, so the harness records when each transcript line arrived in `transcript.times`; runs
  recorded before that file existed show `-`.
- **Generated classes overridden or hand-written**: the number of files in which the agent replaced a class Prefab
  generates or hand-wrote an alternative (controller, repository, persistence code, message listener, JPA entity),
  counted from the guardrail violations, with the number of attempts that had any. Rules a task allows are not
  counted.
- **Contaminated attempts**: the agent read the benchmark's own sources (hidden tests or reference solutions). These
  attempts count as failed.

The summary is followed by a **By tier** table (pass rate with confidence interval, median turns, time to first green
build and overrides per tier), failure counts per category, pass counts per task category, one row per attempt (tier,
checks passed `+` or failed `-`, metrics including first green build and overrides, failure category), and, for every
failure, the evidence: failed checks, guardrail violations and compiler errors. The workspace of a failed attempt is
kept, so you can open it, run Maven and read the diff (`agent.diff`) and the transcript (`transcript.jsonl`).

`compare` puts several runs side by side: pass rate, cost, turns, time to first green build, overrides, the pass rate
of every tier and failure categories per run, then one row per task with `PASS n/n`, `FAIL 0/n` or `FLAKY k/n`, plus
the failure category abbreviations (`doc`, `diag`, `api`, `spring`, `infra`).

## Classifying failures

Every failed attempt gets one category, which says what kind of follow-up it needs:

| Category | Meaning | Typical follow-up |
|---|---|---|
| `DOC_GAP` | The model did not know how to do it with Prefab. | Add or fix docs, cheat sheet entry or recipe. |
| `MISLEADING_DIAGNOSTIC` | A Prefab compiler or runtime message misled the model or did not help it. | Improve the diagnostic. |
| `API_MISUSE` | The model used a Prefab API in an unsupported way or named things differently than asked. | Make the API harder to misuse, or document the rule. |
| `PLAIN_SPRING_FALLBACK` | The model hand-wrote what Prefab generates. | Strengthen the "do not" guidance. |
| `TEST_OR_INFRASTRUCTURE` | The task, its tests or the environment failed, not the model. | Fix the task or the harness. |

Classification happens in three layers. Each later layer wins over the earlier one:

1. **Heuristics** (always on): an agent crash, Docker problem or broken mutant counts as infrastructure. A failed
   guardrail counts as a Spring fallback. An unresolved Prefab processor error counts as a misleading diagnostic.
   Hidden tests that don't compile against the generated API, or a project that doesn't compile, count as API
   misuse. Wrong behaviour counts as a doc gap. Heuristics are a starting point: review the doc-gap ones.
2. **Triage** (`--triage-model claude-sonnet-5-5`): a stronger model reads the prompt, the diff, the agent's final
   message and the evidence, then picks a category with a one-sentence rationale.
3. **Manual review**: write `classifications.yml` next to the report, then re-render it with
   `benchmark/run.sh classify benchmark/results/<run-id>`:

   ```yaml
   events-multicast#1:
     category: API_MISUSE
     note: used @ByReference with a query instead of @Multicast and a repository mixin
   ```

Turn each failure into a backlog task (or attach it to an existing one) and note the attempt key in it, so the next
benchmark run can confirm the fix.

## Adding a task

A task is a directory in `benchmark/tasks/`. Its name is the task id.

```
benchmark/tasks/events-multicast/
├── task.yml        # metadata and acceptance settings
├── prompt.md       # what the agent is asked, as a developer would ask it
├── starter/        # files overlaid on benchmark/template: the starting project
├── hidden/         # acceptance tests, copied in only after the agent finishes
├── reference/      # a correct solution, overlaid on the starter (plus optional .delete list)
└── mutants/        # only for test-writing tasks: one directory per deliberate bug
```

`task.yml`:

```yaml
title: Update many aggregates from one event
category: events-handling      # used for per-category pass rates and --tasks selection
tier: quick                    # quick (default), feature or brownfield
base: shop                     # brownfield tasks only: the application in benchmark/apps the starter is overlaid on
modules: [postgres, kafka]     # postgres, mongodb, kafka, avro, avro-processor, streams, streams-engine, security, openapi
timeoutMinutes: 30             # default 30
maxTurns: 100                  # default 100
allowGuardrails: []            # guardrail rules this task legitimately breaks
acceptance:                    # only needed for test-writing tasks
  agentTests: true
  minAgentTests: 3
  mainReadOnly: true
  unitTestsOnly: false         # true: the agent's tests may not start a Spring context
  mutants: [pay-keeps-status]
notes: >
  Anything maintainers should know, such as a Prefab bug the reference works around.
```

Guidelines:

- **Write the prompt like a developer would.** State the observable contract: package and type names the hidden
  tests rely on, HTTP paths, JSON fields, topics and event components. Do not name Prefab annotations; finding the
  Prefab way is what the benchmark measures.
- **Feature tasks are user stories.** Start from an empty project (a starter only for configuration, such as the
  OAuth2 resource server), give the package, a user story and numbered acceptance criteria in product language, and
  combine several aggregates with events and handlers, security or tenancy. Say where results may lag behind
  (asynchronous handlers), and test that with `Api.eventually`. Give them `timeoutMinutes: 45` and `maxTurns: 150`.
- **Test over HTTP.** Hidden tests are `@IntegrationTest` classes named `*AcceptanceTest`. Use
  `benchmark.support.Api` from `benchmark/acceptance-support` (create, get, put, multipart, read JSON paths, and
  `eventually` for asynchronous event handling). Reference generated types only when the prompt names them, such as an
  event record or `OrderRepository`.
- **Make the reference idiomatic.** It shows how Prefab is meant to be used, and it is the obvious source for
  recipes. If the reference has to work around a Prefab bug, explain it in `notes` and create a backlog task.
- **Mutants must keep the API.** A mutant changes behaviour only. If generated signatures change, the agent's tests
  no longer compile and the mutant check is reported as infrastructure.
- **Validate:** `benchmark/run.sh validate --tasks <id>` must report the task as valid. Then run it with a strong
  model once, to check that the prompt is clear enough to be solvable.
- **When Prefab's public behaviour changes,** update or add the affected tasks in the same change, and rerun
  `validate`.

The harness's unit tests (`mvn -pl benchmark test`) check that the catalogue still has at least 32 quick, 8 feature and
6 brownfield tasks, that the required use cases are covered, that every brownfield task has a base application, and
that no hidden test leaks into a starting project or a base application.

## Adding a brownfield task

A brownfield task changes an application the agent did not write. Base applications live in `benchmark/apps/<name>/`:

```
benchmark/apps/shop/
├── app.yml         # description (and where the snapshot came from) and the Prefab modules it needs
└── project/        # overlaid on benchmark/template before the task's starter
```

`shop` is a snapshot of the core of the showcase (TASK-337): catalog, customers, ordering, invoicing and inventory on
Postgres, Kafka and Spring Security, renamed to `com.example.shop`, with its own integration tests (including the
`Shop` test helper) and its released schema committed as `src/main/resources/db/migration/V1__generated.sql`. It is a
copy, not a link: a brownfield task must start from the same code in every run, or runs months apart are no longer
comparable. Do not change a base application in place; add a new one (for example `shop-2027`) and move tasks to it
deliberately, re-recording their baselines.

To add a task:

1. Set `tier: brownfield` and `base: shop` in `task.yml`. The base application's modules are added to the task's own.
2. Put only what the task changes in `starter/`: an injected bug, a colleague's half-finished change, a failing test.
   Files there replace the base application's files with the same path.
3. Write the prompt as the request a developer would get: a bug report, a change request with compatibility
   constraints, a user story. Say that the application is in production when the schema holds data, and that the
   existing tests must keep passing.
4. Hidden tests may use the base application's generated test clients and test helpers, or `benchmark.support.Api`.
   A hidden test with the same content as a failing test in the starter (under another class name) stops the agent
   from deleting or weakening that test. To check that a schema change keeps existing data, read the migration scripts
   on the classpath (`classpath*:db/migration/*.sql`, which covers committed and generated ones) and assert on the
   statements, for example a `RENAME COLUMN` and no `DROP COLUMN`, or a default on a new `NOT NULL` column: the
   in-memory test backend runs no migrations.
5. `reference/` holds the changed files only. Validate with `benchmark/run.sh validate --tasks <id>` and check in the
   starter attempt's `acceptance.log` that it fails for the intended reason, not a compile error.

## Running on a schedule

`.github/workflows/benchmark.yml` runs the benchmark on GitHub Actions to track regressions between releases. It is
opt-in:

1. Add the repository secret `ANTHROPIC_API_KEY` (an API key, not a subscription: see [Requirements](#requirements)).
2. Start it from the Actions tab ("Agent benchmark", "Run workflow"). By default it runs the quick tier with Haiku and
   the `cheat-sheet` context; the inputs choose another model, context, tasks or number of repeats.
3. To run it every Monday as well, set the repository variable `BENCHMARK_SCHEDULE` to `true`. Without it the
   schedule does nothing.

The job writes the summary of `report.md` to the run's summary page and uploads the report and all attempt logs as an
artifact. Download `report.json` and use `compare` against an earlier run to see which tasks changed. GitHub runners
are not root, so the agent is not isolated from the checkout; contamination detection still marks attempts that read
benchmark sources (see [Isolation](#isolation-and-known-limitations)).

## Reproducibility

The model is the main source of variance. The same setup passes a slightly different set of tasks each time. The
harness removes the other sources: fixed Prefab version from the working tree, pinned template and Testcontainers
images, a fresh workspace, and empty containers per attempt. The local Maven repository is shared.

The measured margin for the same setup is recorded under [Recorded baselines](#recorded-baselines). As a rule of
thumb, treat a difference between two runs as real only when it is larger than that margin **and** the 95%
confidence intervals barely overlap. Use `--repeat 3` for decisions, and compare per task: a task that flips from
`FAIL 0/3` to `PASS 3/3` is a much stronger signal than one more pass in the total.

## Isolation and known limitations

- **Agents search the file system.** In the first runs, Haiku without docs found the Prefab checkout and read a
  task's reference solution; later a local model ran `find /` and read another Prefab application next to the
  checkout. The harness therefore hides, from every attempt: the repository, the folder that holds it (sibling
  checkouts and applications; skipped when that folder is your home directory or `/`), the parent of the run's work
  directory (other runs' workspaces, including `validate` runs with every reference solution) and any `--hide`
  paths. Keep the work directory in a folder of its own (the default `<tmp>/prefab-benchmark/<run-id>` is), and pass
  `--hide` for other places with Prefab sources or benchmark output, such as a source folder higher up or a stray
  checkout under `/tmp`. The run prints what it hides and how.
  - **Linux as root:** the agent runs in a private mount namespace (`unshare --mount`) in which every hidden path is
    covered by an empty tmpfs, and only the attempt's own workspace is visible. Docker keeps working because
    containers are started by the daemon.
  - **macOS:** the harness passes Claude Code's sandbox settings (`--settings`). The agent's shell commands run with
    reads of the hidden paths denied by the operating system (`Operation not permitted`), and its file tools (Read,
    Edit, Grep, Glob) are refused by permission deny rules, written for both the `/private/var` and `/var` (and
    `/tmp`) spellings. The sandbox re-allows the attempt's own workspace and Claude Code temp directory, even when
    they lie in a hidden folder. For the file tools, which have no re-allow, the harness denies every other entry of
    such a folder as found when the attempt starts; a run that starts later in the same folder is still covered by
    the sandbox. Every attempt's Claude Code temp directory lives under `/tmp/prefab-benchmark-agent`, which is hidden
    too, so parallel attempts cannot read each other's tool output. Maven Central, `~/.m2`, the Docker socket and
    local ports stay reachable, so the agent's tests can use Testcontainers as well as the in-memory backend. The
    settings are kept in the attempt's `isolation-arguments.txt`.
  - **Elsewhere** (GitHub runners, Linux without root): isolation is unavailable and the harness warns.

  In every case, tool calls are scanned for the repository path and `benchmark/tasks/`, and an attempt that touches
  them is marked contaminated and fails.
- `run.sh` installs Prefab without the example modules and removes `*-example` artifacts from `~/.m2`. Without that,
  agents decompiled example jars, which application developers don't have.
- The agent can read the Prefab jars in `~/.m2` (classes, generated sources in its own `target/`), like any user.
  It cannot browse the web.
- Attempts in different slots share the local Maven repository and the CPU. A heavily loaded machine makes agent
  time and build time longer, but not pass rates (timeouts are generous).
- The benchmark measures Claude Code as the agent. The model is pluggable through [model profiles](#model-profiles);
  the `Agent` interface is small, so another agent CLI can be added as another implementation.

## Prefab edit/compile/test measurements (TASK-384)

Warm local builds on Temurin JDK 25 / Maven 3.9.11 measured the showcase with:

```bash
mvn --no-transfer-progress -pl examples/showcase-inmemory -am verify
mvn --no-transfer-progress -T 2 -pl examples/showcase-inmemory -am verify
```

Three runs of the first command took 88.00s, 86.03s and 85.93s (median 86.03s). With `-T 2`, they took
64.05s, 64.53s and 64.74s (median 64.53s), a 25.0% reduction. This is measured for this multi-module reactor;
parallelism is not expected to help a single-module application. One `-T 4` run took 63.46s, too small a
difference from `-T 2` to justify the extra concurrency. One offline `-o -T 2` run took 64.77s and did not help.
Three `mvn -N validate` startup/no-op samples had a median of 0.74s.

The showcase used one Spring test context before and after the regression check (cache `missCount=1`; the check
passed after 104 tests, 0 failures, 0 errors and 4 skips). The observed showcase context startup was 1.485s.
The benchmark template's `rest-create-and-get` validation passed its three hidden tests in 3.75s; the unchanged
starter failed as expected in 3.24s.

The measurements do not provide a complete phase-by-phase split. Maven's compiler plugin runs annotation
processing inside the same `javac` invocation as compilation and does not report those durations separately.
JUnit test-report durations and application-context startup are observable, but vary with host load and must not
be subtracted from total Maven wall time as if they were sequential phases. A diagnostic post-change build with
Spring cache DEBUG logging took 65.27s, but is not comparable to the baseline. Two further post-change samples
took 260.24s and 487.79s while the host was heavily loaded; they are not valid performance measurements, so no
post-change median is claimed.

The TASK-343 Haiku 5.5 `cheat-sheet` baseline recorded median 4 Maven builds and 1m19s agent time. No
post-change model run is recorded, so the agent-time/build-count effect of the guidance remains unverified.

## Recorded baselines

Recorded runs live in `benchmark/results/<run-id>/` (`report.md`, `report.json` and, after review,
`classifications.yml`). The per-attempt logs are not committed.

<!-- BASELINES:BEGIN -->
### 2026-11-22: full baseline, 32 tasks

Prefab `0.13.3-SNAPSHOT`, one attempt per task, two attempts in parallel. Comparison:
[`benchmark/results/comparison-2026-11-22.md`](../../benchmark/results/comparison-2026-11-22.md). Failures were reviewed
and classified manually (`classifications.yml` per run).

| Setup | Run | Passed | 95% CI | Cost | Median turns | Median builds | Median agent time |
|---|---|---|---|---|---|---|---|
| Haiku 5.5, `none` | `20261122-082637` | 30/32 | 80-98% | $2.12 | 24 | 6 | 1m51s |
| Haiku 5.5, `full-docs` | `20261122-091726` | 31/32 | 84-99% | $6.42 | 13 | 3 | 1m06s |
| Haiku 5.5, `full-docs` (repeat) | `20261122-235409` | 31/32 | 84-99% | $6.65 | 13 | 4 | 1m10s |
| Haiku 5.5, `docs-folder` | `20261122-191511` | 29/32 | 76-97% | $0.77 | 18 | 4 | 1m22s |
| Sonnet 5.5, `full-docs` (ceiling) | `20261122-195459` | 32/32 | 89-100% | $15.08 | 6 | 2 | 27s |

What it shows:

- **Haiku already builds Prefab applications well.** It passed 29-31 of 32 tasks in every setup, against 32/32 for
  Sonnet. All differences are inside the confidence intervals: on correctness, no setup is measurably better.
- **Docs buy speed and focus, not correctness.** With the full docs in CLAUDE.md, Haiku needs about half the turns
  and builds of the no-docs run, and about 40% less time. Without docs it reverse-engineers Prefab from the jars in
  `~/.m2`. Reading the docs on demand (`docs-folder`) sits in between, at an eighth of the cost of always-loaded docs,
  because the 60k-token CLAUDE.md is re-read on every turn. A compact cheat sheet (TASK-344) should aim for the speed
  of `full-docs` at the cost of `docs-folder`.
- **Without docs, failures are plain-Spring fallbacks.** Haiku hand-wrote a repository and a `@KafkaListener`
  instead of `@Multicast`, and replaced the generated `BookResponse`. With docs, those disappear.
- **The most frequent failure is a Prefab bug, not a model problem.** In three runs (one of them superseded),
  Haiku noticed that generated child endpoints ignore the parent id in the path, so a task is reachable through
  another project's URL. It replaced them with a hand-written controller (TASK-355).
- **Other findings:** sealed JSON events without `@JsonTypeInfo` (doc gap, TASK-354), and a wrong `@Security` import
  that crashes the processor instead of giving a compile error (TASK-351). Two infrastructure failures came from the
  harness bugs listed below, fixed between runs.

**Margin.** Two identical Haiku `full-docs` runs both passed 31/32. Two of 32 tasks flipped between them: one
infrastructure failure (since fixed) and one model choice (`rest-parent-child`). Treat pass-rate differences of up to
two tasks (about 6 percentage points) between single runs as noise, and confirm larger ones per task with
`--repeat 3`. The 95% intervals of single 32-task runs are about ±10 percentage points near these pass rates.

### Superseded: 2026-11-21 first baseline (8-task subset) and run `20261122-065711`

> These runs were made with harness bugs, since fixed: (1) the isolation hid Claude Code's own temp directory behind a
> 1 MB tmpfs, so large tool output failed with "no space left"; (2) build processes the agent left running in the
> background recreated and then removed the slot's test containers in the middle of the hidden tests; (3) Testcontainers
> reuse let parallel slots share, and remove, each other's databases (TASK-356). Their pass rates are not comparable to
> later runs; their qualitative findings (TASK-353, TASK-355) still hold.

Prefab `0.13.3-SNAPSHOT` at commit `26ff283`. One attempt per task. The subset covers REST value objects, range
filters, schema evolution, sealed event hierarchies, multicast handling, public endpoints, binary uploads and
unit-testing events. It was chosen to fit a subscription usage limit; the full 32-task baseline has not been
recorded yet (run it with an API key). Comparison: [`benchmark/results/comparison-2026-11-21.md`](../../benchmark/results/comparison-2026-11-21.md).

| Setup | Passed | 95% CI | Cost | Median turns | Median Maven builds | Median agent time |
|---|---|---|---|---|---|---|
| Haiku 5.5, `none` | 8/8 | 68-100% | $0.96 | 43 | 12 | 5m05s |
| Haiku 5.5, `full-docs` | 7/8 | 53-98% | $1.74 | 12 | 4 | 1m10s |
| Sonnet 5.5, `full-docs` (ceiling) | 6/8 (7/7 excluding infra) | 41-93% | $3.99 | 6 | 3 | 29s |

What it shows:

- **Haiku can build these features with Prefab.** Pass rates are statistically indistinguishable across the three
  setups. The intervals overlap almost completely, so this run gives no evidence that docs improve correctness.
- **Docs buy speed, not correctness, at this size.** Without docs, Haiku decompiles the Prefab jars in `~/.m2`
  and needs 3.5x more turns, 3x more builds and 4x more time. The full docs in CLAUDE.md cost more per run because
  of the 60k-token context (mostly cache reads). That is the trade-off a compact cheat sheet (TASK-344) should
  improve: fewer turns without the 60k tokens.
- **Both models with docs fell into the same trap** on `security-public-endpoints`. They added a second,
  path-scoped `SecurityFilterChain`, which silently removes Prefab's `@ConditionalOnMissingBean` default chain, so
  authenticated writes return 401. Their own tests only checked anonymous requests. Classified `API_MISUSE` and
  added to TASK-353. Haiku without docs replaced the whole chain and passed.
  TASK-353 makes the default chain coexist with path-scoped chains and updates the reference to use
  `HttpSecurityCustomizer` for public reads.
- One `TEST_OR_INFRASTRUCTURE` failure: the Postgres container died during Sonnet's `list-range-filter` hidden tests.

**Margin.** With 8 tasks, one attempt each, the 95% interval is about ±25 percentage points, and with all 32
tasks about ±15. No repeated run of the same setup has been recorded yet, so the empirical run-to-run margin is still
open. Until it is measured, compare setups per task with `--repeat 3`, and treat total pass-rate differences inside
the confidence intervals as noise.
### 2026-11-24: compact cheat sheet (TASK-344), 32 tasks

Prefab `0.13.3-SNAPSHOT`, Haiku 5.5, one attempt per task, three attempts in parallel, run in a cloud container
**without Docker** on the in-memory test backend (TASK-175). Runs: `20261124-104627-claude-haiku-5-5-cheat-sheet`
and `20261124-111212-claude-haiku-5-5-none`.

| Setup | Passed | 95% CI | Cost | Median turns | Median builds | Median agent time |
|---|---|---|---|---|---|---|
| Haiku 5.5, `none` | 21/32 | 48-80% | $1.08 | 25 | 5 | 2m00s |
| Haiku 5.5, `cheat-sheet` | 24/32 | 58-87% | $0.60 | 16 | 4 | 1m19s |

- **The cheat sheet is faster and cheaper.** About a third fewer turns, 35% less time and 45% lower cost than no
  docs, with a pass rate inside the same confidence interval.
- **Plain-Spring fallbacks dropped from 4 attempts to 1** (`events-multicast` fell back in both setups).
  Without docs, `list-full-text-search`, `rest-computed-field` and `rest-parent-child` also failed the guardrails.
- **These pass rates are not comparable with the 2026-11-22 baselines.** Without Docker, hidden tests that need
  real infrastructure (`mixin-custom-sql`, `mongodb-crud`, the Avro tasks) fail in both setups for reasons that
  have nothing to do with the agent. The `none` run was also flagged contaminated on `avro-publish-and-handle`.
  Repeat both setups on a machine with Docker, with `--repeat 3`, before drawing conclusions about correctness.
### 2026-11-26: local Qwen 3.8 27B across documentation contexts (TASK-392), 6 quick tasks

Prefab `0.13.3-SNAPSHOT`, profile `qwen38-local` (Qwen 3.8 27B, Ollama 0.40.16, 128k window, Apple-silicon Mac with
48 GB), one attempt per task and context, one at a time, through a local sandboxed copy of the profile that hides the
user's source folders (TASK-399 stand-in). Tasks: `rest-create-and-get`, `rest-validation`, `rest-parent-child`,
`events-publish-on-create`, `events-multicast`, `mixin-derived-query`. Comparison:
[`benchmark/results/comparison-2026-11-26-qwen38.md`](../../benchmark/results/comparison-2026-11-26-qwen38.md).

| Setup | Run | Passed | Median turns | Median builds | Median agent time | Input tokens |
|---|---|---|---|---|---|---|
| `none` (reference, recorded once) | `20261126-204151` | 6/6 | 72 | 8 | 30m46s | 17.0M |
| `cheat-sheet` | `20261126-155010` | 6/6 | 31 | 5 | 8m32s | 5.0M |
| `recipes` | `20261126-165026` | 6/6 | 33 | 5 | **5m30s** | **3.2M** |
| `local-run` | `20261126-172744` | 6/6 | 31 | 4 | 7m53s | 4.7M |
| `full-docs` | `20261126-182614` | 6/6 | 24 | 7 | 17m52s | 14.9M |

Agent time per task (turns in brackets):

| Task | `none` | `cheat-sheet` | `recipes` | `local-run` | `full-docs` |
|---|---|---|---|---|---|
| `rest-create-and-get` | 90m30s (73) | 5m42s (26) | 4m36s (23) | 14m26s (29) | 15m40s (23) |
| `rest-validation` | 8m31s (41) | 6m01s (25) | 4m22s (20) | 5m58s (29) | 27m07s (24) |
| `rest-parent-child` | 26m21s (70) | 9m29s (35) | 9m25s (41) | 7m33s (43) | 20m05s (38) |
| `events-publish-on-create` | 38m30s (75) | 13m12s (37) | 7m39s (46) | 15m40s (60) | 15m24s (23) |
| `events-multicast` | 35m12s (78) | 17m46s (54) | 6m25s (39) | 8m14s (32) | 44m25s (52) |
| `mixin-derived-query` | 11m35s (45) | 7m36s (26) | 4m20s (26) | 6m09s (27) | 12m26s (21) |

What it shows:

- **Qwen 3.8 passes all six quick tasks in every setup, including without docs.** No task's outcome differs between
  contexts, so these tasks no longer separate setups on pass rate for this model; compare speed here, and use the
  feature and brownfield tiers (TASK-389) for pass rates.
- **Recipes are the best aid measured so far, not yet a clear winner**: fastest on five of six tasks against both the
  cheat sheet and `local-run`, a third less agent time than the cheat sheet alone and the fewest tokens, with the same
  pass rate. With one attempt per cell, five of six happens by chance about 11% of the time, so confirm with
  `--repeat 3` or the feature tier. Because recipes include the cheat sheet and cost nothing in pass rate, `recipes`
  is now the baseline context (decision-014). `local-run` is about as fast as the cheat sheet; starting the app costs
  time on `rest-create-and-get` and `events-publish-on-create`.
- **Full docs cost more than they save on a local model.** They give the fewest turns, but each turn re-reads a ~60k
  token prompt, so median agent time is twice the cheat sheet's and tokens triple.
- **Without docs it still gets there, slowly.** 2.3x the turns of the cheat sheet and 3.6x its agent time; it
  reverse-engineers Prefab from the jars (`javap`) and generated sources. `rest-create-and-get` used 90 of its 90
  minutes.
- **No guardrail, override or contamination flags** in the 30 recorded attempts. Attempts that read the benchmark's
  reference solutions were discarded before recording, and the sandbox was widened until none did (TASK-399).

Before this run, `qwen-local` (Qwen3-Coder 30B) was tried on a ladder of four tasks with the cheat sheet. It failed
every attempt: invented paths outside the workspace, wrong imports read as missing features, validation on record
components, false success claims. That run led to the slimmer profile (`--tools`, no auto-memory), full imports and
validation placement in the cheat sheet, and TASK-398 (a type-level `@Create` compiles silently and
`agent-context.md` lists an endpoint that is not generated). Per-attempt notes are in TASK-392.

### Feature and brownfield tiers: not yet recorded

The 14 feature and brownfield tasks (TASK-377) validate: every reference solution passes and every starting project
fails. They were written and validated in a cloud container that may not start nested Claude Code sessions, so no
model baselines exist yet. Record them with an API key, for example:

```bash
benchmark/run.sh run --model claude-haiku-5-5 --context full-docs --tasks feature,brownfield --repeat 3
```

```bash
benchmark/run.sh run --model claude-haiku-5-5 --context docs-folder --tasks feature,brownfield --repeat 3
```

```bash
benchmark/run.sh run --model claude-sonnet-5-5 --context full-docs --tasks feature,brownfield --repeat 3
```

Writing the tasks already surfaced three problems an agent would hit: the annotation reference used an unsupported
named attribute for `@DbRename` and showed already-quoted `@DbDefaultValue` string values (corrected by TASK-386);
two equality filters on one field with different parameter names do not compile (TASK-387); and a generated migration
re-added a foreign key that an earlier migration declared inline (fixed by TASK-388). Recording the baselines is TASK-389.
<!-- BASELINES:END -->
