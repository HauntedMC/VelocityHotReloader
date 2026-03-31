#!/usr/bin/env bash
set -euo pipefail

BUILD_FILE="build.gradle.kts"
PUSH_REMOTE="origin"
DO_PUSH=false

usage() {
  cat >&2 <<'EOF'
Usage: scripts/bump-version.sh <major|minor|patch> [--push] [--remote <name>]

Examples:
  scripts/bump-version.sh patch
  scripts/bump-version.sh minor --push
  scripts/bump-version.sh major --push --remote origin
EOF
}

if [[ $# -lt 1 ]]; then
  usage
  exit 1
fi

bump_type="$1"
shift

if [[ "$bump_type" != "major" && "$bump_type" != "minor" && "$bump_type" != "patch" ]]; then
  usage
  exit 1
fi

while [[ $# -gt 0 ]]; do
  case "$1" in
    --push)
      DO_PUSH=true
      shift
      ;;
    --remote)
      if [[ $# -lt 2 ]]; then
        echo "--remote requires a value." >&2
        exit 1
      fi
      PUSH_REMOTE="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "This script must be run inside a git repository." >&2
  exit 1
fi

if [[ ! -f "$BUILD_FILE" ]]; then
  echo "$BUILD_FILE not found." >&2
  exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Working tree is not clean. Commit or stash changes before bumping version." >&2
  exit 1
fi

current_version="$(
  sed -nE 's/^[[:space:]]*version[[:space:]]*=[[:space:]]*"([0-9]+\.[0-9]+\.[0-9]+)".*/\1/p' "$BUILD_FILE" | head -n 1
)"

if [[ -z "$current_version" ]]; then
  echo "Could not parse version from $BUILD_FILE." >&2
  exit 1
fi

if [[ ! "$current_version" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  echo "Project version must be a semantic version like 1.2.3." >&2
  exit 1
fi

major="${BASH_REMATCH[1]}"
minor="${BASH_REMATCH[2]}"
patch="${BASH_REMATCH[3]}"

case "$bump_type" in
  major)
    major=$((major + 1))
    minor=0
    patch=0
    ;;
  minor)
    minor=$((minor + 1))
    patch=0
    ;;
  patch)
    patch=$((patch + 1))
    ;;
esac

new_version="${major}.${minor}.${patch}"
new_tag="v${new_version}"

if git rev-parse -q --verify "refs/tags/${new_tag}" >/dev/null 2>&1; then
  echo "Tag ${new_tag} already exists locally." >&2
  exit 1
fi

if git ls-remote --exit-code --tags "$PUSH_REMOTE" "refs/tags/${new_tag}" >/dev/null 2>&1; then
  echo "Tag ${new_tag} already exists on remote '${PUSH_REMOTE}'." >&2
  exit 1
fi

echo "Current version: ${current_version}"
echo "New version: ${new_version}"

sed -i -E "s/^([[:space:]]*version[[:space:]]*=[[:space:]]*\")[0-9]+\.[0-9]+\.[0-9]+(\".*)$/\1${new_version}\2/" "$BUILD_FILE"

git add "$BUILD_FILE"
git commit -m "Bump version to ${new_tag} for release"
git tag "$new_tag"

if [[ "$DO_PUSH" == "true" ]]; then
  current_branch="$(git rev-parse --abbrev-ref HEAD)"
  git push "$PUSH_REMOTE" "$current_branch"
  git push "$PUSH_REMOTE" "$new_tag"
  echo "Version bump committed, tagged, and pushed to ${PUSH_REMOTE}."
else
  echo "Version bump committed and tagged locally."
  echo "Push when ready:"
  echo "  git push ${PUSH_REMOTE} && git push ${PUSH_REMOTE} ${new_tag}"
fi
