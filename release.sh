#!/usr/bin/env bash
set -euo pipefail

usage() {
    echo "Usage: $0 <release-version> <next-development-version>" >&2
    exit 1
}

fail() {
    echo "Error: $*" >&2
    exit 1
}

validate_version() {
    local label="$1"
    local version="$2"

    [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "$label must use x.y.z format."
}

verify_version_state() {
    local expected_project_version="$1"
    local expected_prefab_version="$2"
    local phase="$3"
    local actual_project_version
    local actual_prefab_version

    actual_project_version=$(mvn --no-transfer-progress help:evaluate -Dexpression=project.version -q -DforceStdout)
    actual_prefab_version=$(mvn --no-transfer-progress help:evaluate -Dexpression=prefab.version -q -DforceStdout)

    [[ "$actual_project_version" == "$expected_project_version" ]] || fail "$phase left project.version at $actual_project_version instead of $expected_project_version."
    [[ "$actual_prefab_version" == "$expected_prefab_version" ]] || fail "$phase left prefab.version at $actual_prefab_version instead of $expected_prefab_version."
}

if [ "$#" -ne 2 ]; then
    usage
fi

release_version="$1"
next_development_version="$2"

validate_version "Release version" "$release_version"
validate_version "Next development version" "$next_development_version"
[[ "$release_version" != "$next_development_version" ]] || fail "Release version and next development version must differ."

current_branch=$(git branch --show-current)
[[ "$current_branch" == "main" ]] || fail "Release must run from main, found $current_branch."

[[ -z "$(git status --porcelain)" ]] || fail "Git working tree must be clean before releasing."

git fetch origin main --tags

local_head=$(git rev-parse HEAD)
remote_main_head=$(git rev-parse origin/main)
[[ "$local_head" == "$remote_main_head" ]] || fail "Local HEAD must match origin/main before releasing."

current_version=$(mvn --no-transfer-progress help:evaluate -Dexpression=project.version -q -DforceStdout)
[[ "$current_version" == *-SNAPSHOT ]] || fail "Current project version must end with -SNAPSHOT before releasing."

git rev-parse -q --verify "refs/tags/$release_version" >/dev/null && fail "Tag $release_version already exists locally."
git ls-remote --exit-code --tags origin "refs/tags/$release_version" >/dev/null 2>&1 && fail "Tag $release_version already exists on origin."

mvn --no-transfer-progress versions:set -DnewVersion="$release_version" -DgenerateBackupPoms=false
mvn --no-transfer-progress versions:set-property -Dproperty=prefab.version -DnewVersion="$release_version" -DgenerateBackupPoms=false
verify_version_state "$release_version" "$release_version" "Setting the release version"

if grep -R -n --include pom.xml -- '-SNAPSHOT' . >/dev/null; then
    fail "Snapshot versions remain in pom.xml files after setting the release version."
fi

mvn --no-transfer-progress clean package
git add .
git diff --cached --quiet && fail "Release version update produced no changes to commit."
git commit -m "Release version $release_version"
git tag "$release_version"
git push --dry-run origin main "refs/tags/$release_version" >/dev/null
git push origin main --tags

mvn --no-transfer-progress deploy -DskipTests -P '!development'

next_snapshot_version="$next_development_version-SNAPSHOT"
mvn --no-transfer-progress versions:set -DnewVersion="$next_snapshot_version" -DgenerateBackupPoms=false
mvn --no-transfer-progress versions:set-property -Dproperty=prefab.version -DnewVersion="$next_snapshot_version" -DgenerateBackupPoms=false
verify_version_state "$next_snapshot_version" "$next_snapshot_version" "Setting the next development version"

git add .
git diff --cached --quiet && fail "Next development version update produced no changes to commit."
git commit -m "Start next development iteration"
git push origin main
