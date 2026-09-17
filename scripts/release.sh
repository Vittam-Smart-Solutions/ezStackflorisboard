#!/usr/bin/env bash
# =============================================================================
# release.sh — Conventional-commits-based release automation
#              for ezStackflorisboard
#
# Usage:
#   ./scripts/release.sh [--dry-run]
#
# What it does:
#   1. Pre-checks: clean working directory, warn if not on dev/main/master
#   2. Reads current version from latest v* tag
#   3. Analyzes commits since tag for conventional commit types
#   4. Calculates new semantic version
#   5. Generates CHANGELOG.md entry
#   6. Updates gradle.properties with new version
#   7. Updates CHANGELOG.md (prepends new section)
#   8. Commits: chore(release): v{VERSION}
#   9. Creates annotated tag: v{VERSION}
#  10. Prompts to push (to origin only — this repo also has an `upstream`
#      remote pointing at florisboard/florisboard; never push tags there)
# =============================================================================

set -euo pipefail

DRY_RUN=false
for arg in "$@"; do
    [[ "${arg}" == "--dry-run" ]] && DRY_RUN=true
done

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

info()    { echo -e "${CYAN}[release]${RESET} $*"; }
success() { echo -e "${GREEN}[release]${RESET} $*"; }
warn()    { echo -e "${YELLOW}[release]${RESET} $*"; }
error()   { echo -e "${RED}[release] ERROR:${RESET} $*" >&2; }

[[ "${DRY_RUN}" == "true" ]] && warn "DRY RUN mode — no files will be changed, no commits or tags will be created."

# =============================================================================
# 1. Pre-checks
# =============================================================================
info "Running pre-checks..."

if ! git diff --quiet HEAD || ! git diff --cached --quiet HEAD 2>/dev/null; then
    error "Working directory is not clean. Commit or stash your changes first."
    exit 1
fi

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [[ "${CURRENT_BRANCH}" != "dev" && "${CURRENT_BRANCH}" != "main" && "${CURRENT_BRANCH}" != "master" ]]; then
    warn "You are on branch '${CURRENT_BRANCH}', not dev/main/master."
    read -r -p "$(echo -e "${YELLOW}Continue anyway? [y/N]:${RESET} ")" CONFIRM
    if [[ "${CONFIRM}" != "y" && "${CONFIRM}" != "Y" ]]; then
        info "Aborted."
        exit 0
    fi
fi

# =============================================================================
# 2. Read current version from latest v* tag
# =============================================================================
LATEST_TAG=$(git describe --tags --match "v*" --abbrev=0 2>/dev/null || echo "")

if [[ -z "${LATEST_TAG}" ]]; then
    CURRENT_VERSION="0.0.0"
    info "No release tags found — starting from 0.0.0"
else
    CURRENT_VERSION="${LATEST_TAG#v}"
    info "Current version: ${LATEST_TAG}"
fi

IFS='.' read -r CUR_MAJOR CUR_MINOR CUR_PATCH <<< "${CURRENT_VERSION}"
CUR_MAJOR="${CUR_MAJOR:-0}"
CUR_MINOR="${CUR_MINOR:-0}"
CUR_PATCH="${CUR_PATCH:-0}"

# =============================================================================
# 3. Analyze commits since tag for conventional commit types
# =============================================================================
info "Analyzing commits since ${LATEST_TAG:-the beginning}..."

if [[ -z "${LATEST_TAG}" ]]; then
    COMMITS=$(git log HEAD --max-count=500 --pretty=format:"%s" 2>/dev/null || true)
else
    COMMITS=$(git log "${LATEST_TAG}..HEAD" --pretty=format:"%s" 2>/dev/null || true)
fi

if [[ -z "${COMMITS}" ]]; then
    warn "No commits since ${LATEST_TAG}. Nothing to release."
    exit 0
fi

BUMP="patch"

while IFS= read -r msg; do
    if [[ "${msg}" =~ ^feat!: ]] || [[ "${msg}" =~ BREAKING[[:space:]]CHANGE ]]; then
        BUMP="major"
        break
    fi
done <<< "${COMMITS}"

if [[ "${BUMP}" == "patch" ]]; then
    while IFS= read -r msg; do
        if [[ "${msg}" =~ ^feat: ]]; then
            BUMP="minor"
            break
        fi
    done <<< "${COMMITS}"
fi

info "Determined bump type: ${BUMP}"

# =============================================================================
# 4. Calculate new version
# =============================================================================
NEW_MAJOR="${CUR_MAJOR}"
NEW_MINOR="${CUR_MINOR}"
NEW_PATCH="${CUR_PATCH}"

case "${BUMP}" in
    major)
        NEW_MAJOR=$(( CUR_MAJOR + 1 ))
        NEW_MINOR=0
        NEW_PATCH=0
        ;;
    minor)
        NEW_MINOR=$(( CUR_MINOR + 1 ))
        NEW_PATCH=0
        ;;
    patch)
        NEW_PATCH=$(( CUR_PATCH + 1 ))
        ;;
esac

NEW_VERSION="${NEW_MAJOR}.${NEW_MINOR}.${NEW_PATCH}"
NEW_TAG="v${NEW_VERSION}"

info "New version will be: ${BOLD}${NEW_TAG}${RESET}"

# =============================================================================
# 5. Generate CHANGELOG.md entry (categorized)
# =============================================================================
BUILD_DATE=$(date -u +"%Y-%m-%d")

FEATURES=""
FIXES=""
PERF=""
REFACTORS=""
OTHER=""

while IFS= read -r msg; do
    [[ -z "${msg}" ]] && continue
    if [[ "${msg}" =~ ^feat!: ]]; then
        FEATURES="${FEATURES}- **BREAKING** ${msg#feat!: }"$'\n'
    elif [[ "${msg}" =~ ^feat: ]]; then
        FEATURES="${FEATURES}- ${msg#feat: }"$'\n'
    elif [[ "${msg}" =~ ^fix: ]]; then
        FIXES="${FIXES}- ${msg#fix: }"$'\n'
    elif [[ "${msg}" =~ ^perf: ]]; then
        PERF="${PERF}- ${msg#perf: }"$'\n'
    elif [[ "${msg}" =~ ^refactor: ]]; then
        REFACTORS="${REFACTORS}- ${msg#refactor: }"$'\n'
    else
        if ! [[ "${msg}" =~ ^chore\(release\): ]]; then
            OTHER="${OTHER}- ${msg}"$'\n'
        fi
    fi
done <<< "${COMMITS}"

CHANGELOG_SECTION="## [${NEW_VERSION}] — ${BUILD_DATE}"$'\n'$'\n'

[[ -n "${FEATURES}" ]]  && CHANGELOG_SECTION+="### Features"$'\n'"${FEATURES}"$'\n'
[[ -n "${FIXES}" ]]     && CHANGELOG_SECTION+="### Bug Fixes"$'\n'"${FIXES}"$'\n'
[[ -n "${PERF}" ]]      && CHANGELOG_SECTION+="### Performance"$'\n'"${PERF}"$'\n'
[[ -n "${REFACTORS}" ]] && CHANGELOG_SECTION+="### Refactors"$'\n'"${REFACTORS}"$'\n'
[[ -n "${OTHER}" ]]     && CHANGELOG_SECTION+="### Other"$'\n'"${OTHER}"$'\n'

echo ""
echo -e "${BOLD}=== Changelog Preview ===${RESET}"
echo "${CHANGELOG_SECTION}"
echo -e "${BOLD}=========================${RESET}"
echo ""

if [[ "${DRY_RUN}" == "true" ]]; then
    success "DRY RUN complete. Would create ${NEW_TAG}. No changes made."
    exit 0
fi

read -r -p "$(echo -e "${CYAN}Proceed with release ${BOLD}${NEW_TAG}${RESET}${CYAN}? [y/N]:${RESET} ")" CONFIRM
if [[ "${CONFIRM}" != "y" && "${CONFIRM}" != "Y" ]]; then
    info "Aborted."
    exit 0
fi

# =============================================================================
# 6. Update gradle.properties with new version
# =============================================================================
info "Updating gradle.properties to version=${NEW_VERSION}..."
sed -i "s/^version=.*/version=${NEW_VERSION}/" gradle.properties

# =============================================================================
# 7. Update CHANGELOG.md (prepend new section after header)
# =============================================================================
CHANGELOG_FILE="CHANGELOG.md"

if [[ -f "${CHANGELOG_FILE}" ]]; then
    EXISTING=$(cat "${CHANGELOG_FILE}")
    FIRST_LINE=$(head -n 1 "${CHANGELOG_FILE}")
    if [[ "${FIRST_LINE}" =~ ^# ]]; then
        {
            echo "${FIRST_LINE}"
            echo ""
            echo "${CHANGELOG_SECTION}"
            tail -n +2 "${CHANGELOG_FILE}" | sed '/^$/d' | head -1 > /dev/null; tail -n +2 "${CHANGELOG_FILE}"
        } > "${CHANGELOG_FILE}.tmp" && mv "${CHANGELOG_FILE}.tmp" "${CHANGELOG_FILE}"
    else
        printf '%s\n\n%s' "${CHANGELOG_SECTION}" "${EXISTING}" > "${CHANGELOG_FILE}"
    fi
else
    cat > "${CHANGELOG_FILE}" <<HEREDOC
# Changelog

All notable changes to ezStackflorisboard are documented here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)

${CHANGELOG_SECTION}
HEREDOC
fi

info "CHANGELOG.md updated."

# =============================================================================
# 8. Commit: chore(release): v{VERSION}
# =============================================================================
info "Staging and committing release files..."
git add gradle.properties "${CHANGELOG_FILE}"
git commit -m "chore(release): ${NEW_TAG}"

# =============================================================================
# 9. Create annotated tag
# =============================================================================
info "Creating annotated tag ${NEW_TAG}..."
git tag -a "${NEW_TAG}" -m "Release ${NEW_TAG}"

success "Tagged ${NEW_TAG} on commit $(git rev-parse --short HEAD)."

# =============================================================================
# 10. Prompt to push (origin only — never upstream)
# =============================================================================
echo ""
read -r -p "$(echo -e "${CYAN}Push ${NEW_TAG} and current branch to origin? [y/N]:${RESET} ")" PUSH_CONFIRM
if [[ "${PUSH_CONFIRM}" == "y" || "${PUSH_CONFIRM}" == "Y" ]]; then
    git push origin "${CURRENT_BRANCH}"
    git push origin "${NEW_TAG}"
    success "Pushed branch '${CURRENT_BRANCH}' and tag '${NEW_TAG}' to origin."
else
    info "Skipped push. Run manually:"
    info "  git push origin ${CURRENT_BRANCH} && git push origin ${NEW_TAG}"
fi

success "Release ${NEW_TAG} complete."
