---
id: TASK-392
title: Record the local Qwen baseline across documentation contexts
status: In Progress
assignee: []
created_date: '2026-11-25 12:00'
labels:
  - agents
  - testing
  - documentation
dependencies:
  - TASK-391
references:
  - backlog/docs/benchmark.md
priority: high
type: task
ordinal: 217000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Record the initial benchmark baseline with the `qwen-local` profile (TASK-391) instead of Haiku, so documentation
setups can be compared without spending API tokens. This closes the model-run criteria of TASK-346 (#6, recipes) and
TASK-382 (#7, local-run). Compare setups on the same model; do not compare pass rates between models.

Run on the owner's machine (Ollama with a context window of at least 32k): `--profile qwen-local --repeat 3` with the
contexts `none`, `cheat-sheet`, `recipes`, `local-run` and `full-docs`, starting with a one-task pilot to confirm the
endpoint and tool calling work. Classify every failure and note which tasks differ between contexts; those feed the
Haiku follow-up in TASK-389.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A one-task pilot on `qwen-local` completes through the agent and acceptance steps, confirming endpoint, context window and tool calling
- [ ] #2 Runs for `none`, `cheat-sheet`, `recipes`, `local-run` and `full-docs` with `--repeat 3` are recorded in `benchmark/results/` and classified, with turns, builds and pass rate compared
- [ ] #3 `benchmark.md` records the Qwen baseline and lists the tasks whose outcome differs between contexts, and the TASK-346 and TASK-382 notes reference it
- [ ] #4 If Qwen fails nearly everything or passes nearly everything, the result says so and proposes another model profile instead of drawing conclusions
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. One-task pilot to confirm endpoint, context window and tool calling (AC #1).
2. The full matrix (47 tasks x 5 contexts x 3 = 705 sequential attempts) would take 7-10 days of machine time at
   ~15 min per quick attempt, which the owner cannot spare. First run a small difficulty ladder on `cheat-sheet`
   (`rest-create-and-get` -> `rest-parent-child` -> `events-publish-on-create` -> `feature-library-loans`, one
   attempt each) to see where Qwen goes off the rails, then decide the scope of the context comparison.
3. Open question: `full-docs` (~60k tokens) cannot fit the 32k window next to Claude Code's ~20k system prompt.
   Either raise `OLLAMA_CONTEXT_LENGTH` for all contexts or record `full-docs` as not runnable on this profile.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
**Pilot (2026-11-26, run `20261126-102401-qwen-local-none`).** Ollama 0.40.16 (macOS app), `qwen3-coder:30b`
Q4_K_M, loaded fully in GPU memory with `context_length` 32768. The run went through agent and acceptance: endpoint,
context window and tool calling work (AC #1). Result: `rest-create-and-get` FAIL, `PLAIN_SPRING_FALLBACK`
(guardrails + hidden tests), 46 turns, 9 builds, 15m03s (Haiku `none`: ~2 min). What Qwen did: killed Maven
(`pkill -f mvn`) when the first build was slow downloading dependencies, looked for Prefab in the SDKMAN Maven
install outside the workspace (on macOS the harness cannot hide the host), hand-wrote a JPA entity/controller/DTO,
removed validation annotations instead of adding the dependency, added H2 for its own tests, then stopped its tests
as "taking too long" and reported success.

**Difficulty ladder on `cheat-sheet` (2026-11-26, one attempt each).**

| Step | Task | Run | Result | Turns | Builds | Agent time |
|---|---|---|---|---|---|---|
| 1 | `rest-create-and-get` | `20261126-104823` | FAIL, 2/3 hidden tests | 35 | 10 | 2m27s |
| 2 | `rest-parent-child` | `20261126-105056` | FAIL, 0/3 hidden tests | 52 | 12 | 9m37s |
| 3 | `events-publish-on-create` | `20261126-110041` | FAIL, hidden tests do not compile | 49 | 7 | 2m44s |
| 4 | `feature-library-loans` | (stopped, not kept) | stopped after ~5 min | | | |

- With the cheat sheet Qwen stays on Prefab (guardrails pass in both), unlike the pilot without docs.
- Step 1 (`API_MISUSE`, the heuristic said `DOC_GAP`): `@NotBlank` placed on record components instead of the `@Create`
  parameters, so a missing title returned 201; `@Max` on a String instead of `@Size`. The cheat sheet states the rule.
  Possible Prefab improvement: warn when validation annotations sit on record components but not on `@Create`
  parameters.
- Step 2 is where Qwen goes off the rails. It first modelled `Task` without a parent, noticed from the generated
  controller that endpoints were at `/tasks`, and added `@Parent Reference<Project>` itself. It then imported
  `@Parent` from `be.appify.prefab.core.annotations` (it lives in `...annotations.rest`), read the compile error as
  "`@Parent` is not available in this version of Prefab", removed it to get a green build and reported a nested
  resource it had not built. All three hidden tests got 404. Classified `DOC_GAP`: the cheat sheet's import line
  lists `.rest` annotations with "…" and does not name `@Parent`.
- Step 3 (`TEST_OR_INFRASTRUCTURE` in spirit, the heuristic said `API_MISUSE`): the plan was right (`@Event` record +
  `EventPublisher.bind`), but `agent.diff` is empty. Qwen built absolute paths from Claude Code's project-memory
  directory name (`~/.claude/projects/-private-var-...-events-publish-on-create-1-project/...`, a dash where the
  workspace has `/project`), so `Read`/`Edit` failed and `Write` created `OrderPlaced.java` in the user's home
  (removed by hand). Maven's "Nothing to compile" was read as Prefab caching. It reported success twice. On macOS the
  harness cannot sandbox the agent, so writes outside the workspace go unnoticed; worth a guardrail.
- Behaviour seen on every attempt: false success claims after giving up on its own tests; writing to `/src/...` at
  the filesystem root; calling tools it was not asked to (`ReportFindings`, unknown skills such as `finish`,
  `project-summary`), each error triggering another identical closing summary.
- Step 4 hit the same path problem before writing any code ("we're working in a read-only environment", "writing
  files in memory") and was stopped to save up to 2h15m of machine time; its results were not kept.

**Conclusion so far (AC #4).** Qwen 3 Coder 30B on `qwen-local` failed all 5 attempts (pilot + ladder), so it does
not yet discriminate between documentation setups; no conclusions about the docs are drawn from it. The cheat sheet
does keep it on Prefab (guardrails passed in steps 1-3, against a plain-Spring fallback without docs), and the
step 1 and 2 failures point at concrete, small doc fixes. But from step 3 on, tool use (wrong absolute paths) fails
before Prefab knowledge matters.

**Smaller agent prompt (2026-11-26).** Measured on a one-line prompt against Ollama: Claude Code's default
starting prompt is ~11.8k tokens (20 tools, 44 skills/commands; the ladder's longest prompts reached 30-42k, past the
32k window). `--tools Bash,Read,Edit,Write` brings it to ~3.9k; `--disable-slash-commands` saves nothing. Asked to
quote its working directory, Qwen quoted the auto-memory path `~/.claude/projects/<mangled path>/memory/` in both
setups: that, not truncation alone, is where the invented `...-1-project` paths came from.
`CLAUDE_CODE_DISABLE_AUTO_MEMORY=1` (or `autoMemoryEnabled: false`) removes it and brings the prompt to ~3.4k.
Both are now in `benchmark/models/qwen-local.yml` only, so hosted baselines stay comparable. Next: re-run ladder
steps 1-3 with the slim profile at 32k before considering a larger window.

**Slim-profile ladder (2026-11-26, run `20261126-114358`, stop at first failure).** Step 1 `rest-create-and-get`
FAIL, 1/3 hidden tests, 54 turns, 12 builds, 2m52s. No more memory-directory paths, and all files landed in the
workspace; Qwen still passed `/src/...` (root-relative) to `Read`/`Write` until `pwd` set it straight. It imported
`be.appify.prefab.core.Reference` (actual: `be.appify.prefab.core.service.Reference`), decided `Reference` was
generated by the processor, and replaced it with `@Id String id` set to `null`: `POST /books` returned 500. Validation
again sat on record components, not `@Create` parameters. It deleted its own failing test and looped "final
summary" + another verification command until it stopped. Second import miss of the same kind (`@Parent` before):
the cheat sheet should give the full import of every type it uses.

Proposal before running the context matrix:
1. Re-run the ladder with a larger Ollama context window (`OLLAMA_CONTEXT_LENGTH=131072`). Input per turn was
   ~20-22k tokens against a 32k window; Ollama truncates the oldest tokens when a prompt overflows, which would drop
   the system prompt that names the working directory and could explain the invented paths. It is also needed for
   `full-docs` (~60k tokens) to be measured at all.
2. If paths still go wrong, try a second local profile with `qwen3.6:27b` (already pulled, supports tools and
   thinking) before falling back to a hosted model.
3. Only then run the five contexts with `--repeat 3`, on a task subset the owner can afford in machine time
   (quick attempts take 2-15 min on this machine, feature tasks far longer).

Findings for follow-up:
- Cheat sheet: name `@Parent` in the `be.appify.prefab.core.annotations.rest` import list (step 2).
- Prefab: warn when Jakarta validation sits on record components but not on `@Create` parameters (step 1).
- Harness: on macOS nothing stops an agent from writing outside its workspace; detect or flag such writes (step 3).
- The report shows `Cost $2,06` for a local model: Claude Code prices it at its own API rates. Not meaningful for a
  local model, whose real cost is the energy used. Filed as TASK-393.
<!-- SECTION:NOTES:END -->
