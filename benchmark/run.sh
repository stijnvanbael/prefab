#!/usr/bin/env bash
# Runs the Prefab agent benchmark. See backlog/docs/benchmark.md.
#
#   benchmark/run.sh list
#   benchmark/run.sh validate
#   benchmark/run.sh run --model claude-haiku-5-5 --context full-docs
#
# Set BENCHMARK_SKIP_INSTALL=1 to skip installing the current Prefab version into the local Maven repository.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MVN="${BENCHMARK_MVN:-mvn}"
CLASSPATH_FILE="$ROOT/benchmark/target/classpath.txt"

if [[ "${BENCHMARK_SKIP_INSTALL:-0}" != "1" && "${1:-}" =~ ^(run|validate)$ ]]; then
  echo "Installing the current Prefab version into the local Maven repository (BENCHMARK_SKIP_INSTALL=1 skips this)..."
  # Example modules are not installed: agents would find and decompile them, which application developers can't.
  "$MVN" -q -B -ntp -f "$ROOT/pom.xml" install -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip \
    -pl '!examples/avro,!examples/kafka,!examples/streams,!examples/pubsub,!examples/sns-sqs,!examples/mongodb,!examples/showcase,!examples/showcase-variant-parent,!examples/showcase-mongodb,!benchmark'
  rm -rf "$HOME"/.m2/repository/be/appify/prefab/*-example "$HOME"/.m2/repository/be/appify/prefab/*showcase*
else
  "$MVN" -q -B -ntp -f "$ROOT/benchmark/pom.xml" compile
fi
"$MVN" -q -B -ntp -f "$ROOT/benchmark/pom.xml" dependency:build-classpath -Dmdep.outputFile="$CLASSPATH_FILE" >/dev/null

cd "$ROOT"
exec java -cp "$ROOT/benchmark/target/classes:$(cat "$CLASSPATH_FILE")" be.appify.prefab.benchmark.BenchmarkCli "$@"
