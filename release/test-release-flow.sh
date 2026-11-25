#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
work=$(mktemp -d)
trap 'rm -f -- "$work/bin/git" "$work/bin/mvn" "$work/bin/javac" "$work/bin/java" "$work/release.sh" "$work/trace" "$work/version"; rmdir -- "$work/bin" "$work"' EXIT
mkdir -p "$work/bin"
cp "$root/release.sh" "$work/release.sh"

cat > "$work/bin/git" <<'EOF'
#!/usr/bin/env bash
echo "git $*" >> "$TRACE"
case "$1" in
    branch) echo main ;;
    status) ;;
    rev-parse)
        if [[ "$*" == *"refs/tags/"* ]]; then exit 1; fi
        echo abc123
        ;;
    ls-remote) exit 1 ;;
    diff) exit 1 ;;
esac
EOF

cat > "$work/bin/mvn" <<'EOF'
#!/usr/bin/env bash
echo "mvn $*" >> "$TRACE"
case "$*" in
    *help:evaluate*) cat "$VERSION_FILE" ;;
    *versions:set\ *)
        for arg in "$@"; do
            case "$arg" in -DnewVersion=*) printf '%s\n' "${arg#*=}" > "$VERSION_FILE";; esac
        done
        ;;
esac
EOF

cat > "$work/bin/javac" <<'EOF'
#!/usr/bin/env bash
echo "javac $*" >> "$TRACE"
EOF

cat > "$work/bin/java" <<'EOF'
#!/usr/bin/env bash
echo "java $*" >> "$TRACE"
if [[ "$*" == *"ReleasePublisher verify"* && "${FAIL_VERIFY:-false}" == true ]]; then
    exit 1
fi
EOF
chmod +x "$work/bin/git" "$work/bin/mvn" "$work/bin/javac" "$work/bin/java"
export PATH="$work/bin:$PATH" TRACE="$work/trace" VERSION_FILE="$work/version"

cd "$work"
printf '0.13.3-SNAPSHOT\n' > "$VERSION_FILE"
export FAIL_VERIFY=true
if bash release.sh 0.13.3 0.13.4; then
    echo "Expected a failed bundle verification" >&2
    exit 1
fi
if grep -Eq '^git (add|commit|tag|push)|^java .*ReleasePublisher upload' "$TRACE"; then
    echo "Release refs or upload occurred after verification failed" >&2
    exit 1
fi

: > "$TRACE"
printf '0.13.3-SNAPSHOT\n' > "$VERSION_FILE"
export FAIL_VERIFY=false
bash release.sh 0.13.3 0.13.4
verify=$(grep -n 'ReleasePublisher verify' "$TRACE" | cut -d: -f1)
commit=$(grep -n '^git commit -m Release version' "$TRACE" | cut -d: -f1)
tag=$(grep -n '^git tag 0.13.3' "$TRACE" | cut -d: -f1)
upload=$(grep -n 'ReleasePublisher upload' "$TRACE" | cut -d: -f1)
if ! (( verify < commit && commit < tag && tag < upload )); then
    echo "Release guard did not run before ref creation and upload" >&2
    exit 1
fi
echo "Release flow tests passed"
