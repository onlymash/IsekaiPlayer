#!/bin/bash
#
# IsekaiPlayer - Sovereign above myriad realms; shatter every mortal cipher.
# Copyright (C) 2026 onlymash
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

set -e

PROJECT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$PROJECT_ROOT"

show_help() {
    echo "IsekaiPlayer GitHub Release Publisher"
    echo ""
    echo "Usage:"
    echo "  $0 <tag> [type]"
    echo ""
    echo "Release Types:"
    echo "  prerelease   Mark as pre-release (default)"
    echo "  release      Mark as full release"
    echo ""
    echo "Options:"
    echo "  -h, --help   Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 1.0.0.beta06             # Defaults to prerelease"
    echo "  $0 1.0.0.beta06 prerelease  # Explicit prerelease"
    echo "  $0 1.0.0 release            # Full release"
}

if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_help
    exit 0
fi

TAG="$1"
TYPE_PARAM="${2:-prerelease}"

if [ -z "$TAG" ]; then
    echo "Error: Release tag parameter is required." >&2
    echo "Usage: $0 <tag> [type]" >&2
    echo "Example: $0 1.0.0.beta06 [prerelease|release]" >&2
    exit 1
fi

case "$TYPE_PARAM" in
    prerelease|pre|--prerelease|-p)
        RELEASE_TYPE="prerelease"
        ;;
    release|prod|full|--release|-r)
        RELEASE_TYPE="release"
        ;;
    *)
        echo "Error: Invalid release type '$TYPE_PARAM'." >&2
        echo "Supported release types: 'prerelease' (default) or 'release'." >&2
        exit 1
        ;;
esac

if ! command -v gh &> /dev/null; then
    echo "Error: 'gh' (GitHub CLI) is not installed or not found in PATH." >&2
    exit 1
fi

if ! git rev-parse --verify "refs/tags/$TAG" >/dev/null 2>&1; then
    echo "Error: Git tag '$TAG' does not exist." >&2
    echo "Please create the git tag first (e.g. git tag $TAG)." >&2
    exit 1
fi

shopt -s nullglob
APK_FILES=("$PROJECT_ROOT"/app/build/outputs/apk/release/IsekaiPlayer-*.apk)
shopt -u nullglob

if [ ${#APK_FILES[@]} -eq 0 ]; then
    echo "Error: No release APK found matching 'app/build/outputs/apk/release/IsekaiPlayer-*.apk'." >&2
    echo "Please compile the release APK first (e.g. ./gradlew :app:assembleRelease)." >&2
    exit 1
fi

GH_CREATE_FLAGS=("$TAG" "--generate-notes")
if [ "$RELEASE_TYPE" = "prerelease" ]; then
    GH_CREATE_FLAGS+=("--prerelease")
fi

echo "==> Creating GitHub $RELEASE_TYPE for tag: $TAG..."
gh release create "${GH_CREATE_FLAGS[@]}"

echo "==> Uploading release APK(s)..."
gh release upload "$TAG" "${APK_FILES[@]}"

echo "==> GitHub $RELEASE_TYPE '$TAG' published successfully!"
