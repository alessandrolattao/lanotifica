# LaNotifica — Project Guide for Claude

## What is this project

LaNotifica forwards Android push notifications to a Linux desktop via D-Bus.
Two components:

- **`server/`** — Go HTTPS server (TLS self-signed, mDNS discovery, D-Bus notifications, icon cache)
- **`app/`** — Android app (Compose UI, sends notifications to the server via HTTPS + Bearer token)

End-to-end flow: Android detects a notification → sends POST `/notification` to the Go server → server calls D-Bus → notification appears on Linux desktop. Server discovery is automatic via mDNS (`lanotifica.local`). Pairing happens by scanning a QR code on the server's web page (PIN-protected since v1.1.0).

## Key skills — read these before working

- `/project-overview` — architecture, data flow, package structure
- `/go-server-structure` — Go package layout, conventions, patterns
- `/android-app-structure` — Android architecture (MVVM, DataStore, Tink)
- `/build-and-packaging` — Makefile, RPM/DEB, COPR, Play Store
- `/bump-version` — HOW TO bump version before tagging a release
- `/release` — pre-release checklist (run this before every tag)
- `/lint-and-test` — run linters and tests locally
- `/new-feature` — where to add code for new features
- `/review-security` — security checklist for Go server changes

## Build

```bash
# Go server
make build           # builds bin/lanotifica
make test-server     # go test ./...
make lint-server     # golangci-lint (config in server/.golangci.yml)

# Android
make test-app        # gradle test
make lint-app        # gradle lint + spotless
```

## Versioning — CRITICAL

Three files must be in sync before tagging. Run `/bump-version` for the checklist:

1. `app/app/build.gradle.kts` — `versionCode` (monotone int) + `versionName` (semver)
2. `packaging/rpm/lanotifica.spec` — uses `%{pkg_version}` (injected by Makefile, do NOT hardcode)
3. Git tag `vX.Y.Z` — triggers the release workflow

## Code conventions (Go server)

- All errors wrapped: `fmt.Errorf("context: %w", err)`
- No `//nolint` except for genuine false positives (gosec on controlled paths), always with explanation
- All tests use `t.Parallel()`, subtests too
- Config follows XDG Base Dir spec (`~/.config/lanotifica/`)
- `defer func() { _ = f.Close() }()` for resource cleanup in defers

## CI / CD

- **CI** (`.github/workflows/ci.yml`): runs on every push to non-tag branches. Tests + lint for Go and Android.
- **Release** (`.github/workflows/release.yml`): triggered on `v*` tags. Builds RPM, DEB, AAB, APK. Creates GitHub Release. Deploys to Play Store internal track. COPR is handled by Packit automatically.

## GitHub Actions versions (current)

- `actions/checkout@v7`
- `actions/setup-go@v6`
- `actions/setup-java@v5`
- `actions/upload-artifact@v7`
- `actions/download-artifact@v8`
- `softprops/action-gh-release@v3`
- `golangci/golangci-lint-action@v9`
- `r0adkll/upload-google-play@v1`
