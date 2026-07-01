#!/usr/bin/env bash
# Publish lanotifica-bin to AUR.
# Usage: ./publish-aur.sh <version>
# Example: ./publish-aur.sh 1.2.0
set -euo pipefail

VERSION="${1:?usage: $0 <version>}"
TARBALL_URL="https://github.com/alessandrolattao/lanotifica/releases/download/v${VERSION}/lanotifica-linux-amd64.tar.gz"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AUR_REPO="ssh://aur@aur.archlinux.org/lanotifica-bin.git"
WORK_DIR="$(mktemp -d)"

cleanup() { rm -rf "${WORK_DIR}"; }
trap cleanup EXIT

echo "==> Publishing lanotifica-bin v${VERSION} to AUR"

# 1. Compute sha256
echo "==> Downloading tarball and computing sha256..."
SHA256=$(curl -fsSL "${TARBALL_URL}" | sha256sum | awk '{print $1}')
echo "    sha256: ${SHA256}"

# 2. Update PKGBUILD
echo "==> Updating PKGBUILD..."
sed -i "s/^pkgver=.*/pkgver=${VERSION}/" "${SCRIPT_DIR}/PKGBUILD"
sed -i "s/^pkgrel=.*/pkgrel=1/" "${SCRIPT_DIR}/PKGBUILD"
sed -i "s/sha256sums=.*/sha256sums=('${SHA256}')/" "${SCRIPT_DIR}/PKGBUILD"

# 3. Regenerate .SRCINFO via Docker
echo "==> Generating .SRCINFO via Docker (Arch Linux)..."
docker run --rm \
  -v "${SCRIPT_DIR}:/pkg" \
  archlinux:latest \
  bash -c "useradd -m builder && chown -R builder /pkg && su builder -c 'cd /pkg && makepkg --printsrcinfo'" \
  > "${SCRIPT_DIR}/.SRCINFO"
echo "    .SRCINFO generated"

# 4. Clone AUR repo and publish
echo "==> Cloning AUR repo..."
git clone "${AUR_REPO}" "${WORK_DIR}/aur-lanotifica-bin"
cp "${SCRIPT_DIR}/PKGBUILD" "${WORK_DIR}/aur-lanotifica-bin/"
cp "${SCRIPT_DIR}/lanotifica.install" "${WORK_DIR}/aur-lanotifica-bin/"
cp "${SCRIPT_DIR}/.SRCINFO" "${WORK_DIR}/aur-lanotifica-bin/"

echo "==> Committing and pushing to AUR..."
cd "${WORK_DIR}/aur-lanotifica-bin"
git add PKGBUILD lanotifica.install .SRCINFO
git commit -m "Release v${VERSION}"
git push

echo "==> Done! https://aur.archlinux.org/packages/lanotifica-bin"
