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
TARGET_FILE="$PROJECT_ROOT/player/src/main/assets/cacert.pem"
CACERT_URL="https://curl.se/ca/cacert.pem"
SHA256_URL="https://curl.se/ca/cacert.pem.sha256"

show_help() {
    echo "CA Certificate Bundle Updater"
    echo ""
    echo "Downloads the latest Mozilla CA root certificates bundle from curl.se"
    echo "and updates player/src/main/assets/cacert.pem."
    echo ""
    echo "Usage:"
    echo "  $0 [options]"
    echo ""
    echo "Options:"
    echo "  -f, --force    Force update even if the SHA256 checksum has not changed"
    echo "  -h, --help     Show this help message"
}

FORCE=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        -f|--force)
            FORCE=1
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            echo "Error: Unknown option $1" >&2
            show_help
            exit 1
            ;;
    esac
done

get_sha256() {
    local file=$1
    if command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$file" | awk '{print $1}'
    elif command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$file" | awk '{print $1}'
    else
        echo ""
    fi
}

echo ">>> Updating CA Certificate bundle..."

TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

TMP_CACERT="$TMP_DIR/cacert.pem"
TMP_SHA="$TMP_DIR/cacert.pem.sha256"

# 1. Download SHA256 checksum
echo "    Fetching expected SHA256 checksum..."
if ! curl -sSL -f -o "$TMP_SHA" "$SHA256_URL"; then
    echo "Error: Failed to fetch SHA256 checksum from $SHA256_URL" >&2
    exit 1
fi

EXPECTED_SHA=$(awk '{print $1}' "$TMP_SHA")

# Check if current file matches expected SHA256
if [ -f "$TARGET_FILE" ] && [ "$FORCE" -eq 0 ]; then
    CURRENT_SHA=$(get_sha256 "$TARGET_FILE")
    if [ -n "$CURRENT_SHA" ] && [ "$CURRENT_SHA" == "$EXPECTED_SHA" ]; then
        echo "    cacert.pem is already up to date (SHA256: $CURRENT_SHA)."
        exit 0
    fi
fi

# 2. Download cacert.pem
echo "    Downloading latest cacert.pem from $CACERT_URL..."
if ! curl -sSL -f -o "$TMP_CACERT" "$CACERT_URL"; then
    echo "Error: Failed to download CA bundle from $CACERT_URL" >&2
    exit 1
fi

# 3. Verify SHA256 checksum of downloaded file
COMPUTED_SHA=$(get_sha256 "$TMP_CACERT")

if [ -n "$EXPECTED_SHA" ] && [ "$COMPUTED_SHA" != "$EXPECTED_SHA" ]; then
    echo "Error: SHA256 verification failed!" >&2
    echo "  Expected: $EXPECTED_SHA" >&2
    echo "  Computed: $COMPUTED_SHA" >&2
    exit 1
fi

# 4. Extract release date from file header
DATE_INFO=$(grep -m 1 "Certificate data from Mozilla as of:" "$TMP_CACERT" | sed 's/## Certificate data from Mozilla as of: //' || echo "Unknown date")

# 5. Move to target destination
mkdir -p "$(dirname "$TARGET_FILE")"
mv "$TMP_CACERT" "$TARGET_FILE"

echo ">>> Successfully updated $TARGET_FILE"
echo "    Mozilla Release Date: $DATE_INFO"
echo "    SHA256:               $COMPUTED_SHA"
