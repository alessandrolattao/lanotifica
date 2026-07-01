---
name: release
description: Use when cutting a new release: creating the git tag, triggering the release pipeline, or debugging release failures.
---

# Release

## Pre-release checklist

Before creating the tag:

- [ ] CI is green on `main` (check GitHub Actions)
- [ ] All three version sync points updated: `build.gradle.kts` (versionCode + versionName), RPM spec (`%{pkg_version}`), Go Version constant
- [ ] `make lint` passes locally (golangci-lint + Android Lint + Detekt + Spotless)
- [ ] `make test` passes locally (Go tests + Android unit tests)
- [ ] Version commit is on `main` and pushed

## Creating the release

```bash
# Create annotated tag
git tag -a vX.Y.Z -m "Release vX.Y.Z"
git push origin vX.Y.Z
```

This triggers `.github/workflows/release.yml` automatically.

## What the release pipeline does

1. Tests + lint (Go + Android)
2. Builds RPM (`make rpm VERSION=X.Y.Z`) and DEB (`make deb VERSION=X.Y.Z`)
3. Builds Android AAB + APK (release-signed)
4. Creates GitHub Release with all artifacts
5. Uploads AAB to Play Store internal track
6. Packit picks up the GitHub Release and builds for COPR/Fedora (automatic, no action needed)

## Debugging release failures

**RPM build fails with wrong version:**
Check `packaging/rpm/lanotifica.spec` — `Version:` must be `%{pkg_version}`, not hardcoded.

**Play Store rejects AAB (versionCode already used):**
Bump `versionCode` in `build.gradle.kts`, commit, move the tag.

**Go build fails:**
Run `golangci-lint run ./...` locally to catch before pushing.

**Moving a tag after a fix:**
```bash
git tag -d vX.Y.Z
git push origin :refs/tags/vX.Y.Z
# Fix the issue, commit, push to main
git tag -a vX.Y.Z -m "Release vX.Y.Z"
git push origin vX.Y.Z
```

## COPR (Fedora packaging)

Handled by Packit automatically. Config in `.packit.yaml`. If COPR fails, check the Packit dashboard — it's usually a spec file issue.

## AUR (Arch Linux packaging)

AUR is NOT automatic — must be published manually after each release.

### First time (one-off setup)
1. Create account on aur.archlinux.org and add SSH public key
2. `git clone ssh://aur@aur.archlinux.org/lanotifica-bin.git`

### After every release (vX.Y.Z)

```bash
# One command — handles sha256, .SRCINFO via Docker, AUR push
./packaging/arch/publish-aur.sh X.Y.Z
```

The script (`packaging/arch/publish-aur.sh`):
1. Downloads the tarball from GitHub and computes sha256
2. Updates PKGBUILD (pkgver, sha256sums, provides)
3. Regenerates `.SRCINFO` via Docker (Arch Linux image)
4. Clones the AUR repo, commits and pushes

Requires: `docker`, `curl`, SSH key on aur.archlinux.org

### Notes
- `packaging/arch/PKGBUILD` is the source of truth; AUR repo is just the publication target
- `sha256sums=('SKIP')` in the repo is a placeholder — replace with real hash before AUR push
- If AUR is down, publish later: the GitHub release tarball is permanent
