#!/usr/bin/env bash
# Bump version across all sync points and commit.
# Usage: ./scripts/bump-version.sh <version>
# Example: ./scripts/bump-version.sh 1.3.0
set -euo pipefail

VERSION="${1:?usage: $0 <version>}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE="${ROOT}/app/app/build.gradle.kts"
PKGBUILD="${ROOT}/packaging/arch/PKGBUILD"
SPEC="${ROOT}/packaging/rpm/lanotifica.spec"

# Validate semver format
if ! [[ "${VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Error: version must be semver (e.g. 1.3.0), got: ${VERSION}"
    exit 1
fi

echo "==> Bumping version to ${VERSION}"

# 1. build.gradle.kts: versionName + versionCode (+1)
CURRENT_CODE=$(grep -oP 'versionCode\s*=\s*\K[0-9]+' "${GRADLE}")
NEW_CODE=$(( CURRENT_CODE + 1 ))
sed -i "s/versionCode = ${CURRENT_CODE}/versionCode = ${NEW_CODE}/" "${GRADLE}"
sed -i "s/versionName = \"[^\"]*\"/versionName = \"${VERSION}\"/" "${GRADLE}"
echo "    build.gradle.kts: versionCode ${CURRENT_CODE} → ${NEW_CODE}, versionName → ${VERSION}"

# 2. RPM spec: must use %{pkg_version}, never hardcoded
if grep -q "^Version:.*%{pkg_version}" "${SPEC}"; then
    echo "    lanotifica.spec: OK (uses %{pkg_version})"
else
    echo "Error: lanotifica.spec has a hardcoded Version — fix it before bumping"
    exit 1
fi

# 3. PKGBUILD: pkgver (provides uses ${pkgver} variable, no change needed)
sed -i "s/^pkgver=.*/pkgver=${VERSION}/" "${PKGBUILD}"
echo "    PKGBUILD: pkgver → ${VERSION}"

# 4. Commit
git -C "${ROOT}" add \
    app/app/build.gradle.kts \
    packaging/arch/PKGBUILD
git -C "${ROOT}" commit -m "chore: bump version to v${VERSION}"

echo "==> Done. Next: make lint && make test, then tag:"
echo "    git tag -a v${VERSION} -m \"Release v${VERSION}\" && git push origin v${VERSION}"
