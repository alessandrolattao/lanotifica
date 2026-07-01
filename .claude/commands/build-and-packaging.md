---
name: build-and-packaging
description: Use when running builds locally, touching CI/CD workflows, adding packaging steps, or debugging release pipeline failures.
---

# Build and Packaging

## Makefile targets

```bash
make build              # Go server binary → bin/lanotifica
make test-server        # go test ./... in server/
make test-app           # Gradle unit tests
make lint-server        # golangci-lint in server/
make lint-app           # Gradle lint + Spotless check
make format             # gofmt + goimports + Spotless
make up                 # air hot reload (dev only)
make rpm VERSION=1.2.0  # builds RPM package
make deb VERSION=1.2.0  # builds DEB package
```

## Go binary versioning

Version injected at build time:
```bash
go build -ldflags "-X main.Version=1.2.0" ./cmd/lanotifica
```

## RPM packaging

```bash
make rpm VERSION=1.2.0
# → rpmbuild --define "pkg_version 1.2.0" packaging/rpm/lanotifica.spec
# → output: ~/rpmbuild/RPMS/x86_64/lanotifica-1.2.0-1.*.rpm
```

**CRITICAL:** `packaging/rpm/lanotifica.spec` must use `%{pkg_version}` — never hardcode the version in the spec file. The `Version:` field must be `%{pkg_version}`.

## DEB packaging

```bash
make deb VERSION=1.2.0
# → sed replaces ${VERSION} in packaging/control template
```

## CI/CD workflows

**CI** (`.github/workflows/ci.yml`) — triggers on every push to non-tag branches:
1. Test Server (Go): `go test` + `golangci-lint`
2. Test Android: lint + unit tests
3. Build Server: `make build`
4. Build Android: `./gradlew assembleDebug`

**Release** (`.github/workflows/release.yml`) — triggers on `v*` tags:
1. Test → Build (parallel Go + Android)
2. GitHub Release with RPM, DEB, AAB, APK artifacts
3. Deploy to Play Store internal track

**COPR** (Fedora/EPEL): handled automatically by Packit on each GitHub Release. Config in `.packit.yaml`. No manual action needed.

## Current GitHub Actions versions

| Action | Version |
|--------|---------|
| actions/checkout | v7 |
| actions/setup-go | v6 |
| actions/setup-java | v5 |
| actions/upload-artifact | v7 |
| actions/download-artifact | v8 |
| softprops/action-gh-release | v3 |
| golangci/golangci-lint-action | v9 |
| r0adkll/upload-google-play | v1 |

To check for newer versions: `gh release view --repo <owner>/<action-repo>`
