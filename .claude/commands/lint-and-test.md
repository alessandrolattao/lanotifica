---
name: lint-and-test
description: Use when running tests or lint, fixing lint violations, or before committing/pushing changes.
---

# Lint and Test

## Go server

```bash
cd server

# Run tests
go test ./...

# Run linter
golangci-lint run ./...

# Run with auto-fix where possible
golangci-lint run --fix ./...
```

Config: `server/.golangci.yml` (golangci-lint v2, strict).

## Android app

```bash
cd app

# Format (Spotless) — always run before lint
./gradlew spotlessApply

# Android Lint + Detekt + Spotless check
./gradlew lint detekt spotlessCheck

# Unit tests
./gradlew test
```

Detekt config: `app/config/detekt/detekt.yml`.

## Before pushing: run everything via make

```bash
make lint   # golangci-lint + Android Lint + Detekt + Spotless
make test   # go test + Android unit tests
```

Lefthook runs this automatically on `git push` (install once with `lefthook install`).

## Common lint violations and how to fix them

**wrapcheck** — error not wrapped with context:
```go
// bad
return err
// good
return fmt.Errorf("doing thing: %w", err)
```

**tparallel** — test or subtest missing `t.Parallel()`:
```go
func TestFoo(t *testing.T) {
    t.Parallel()
    t.Run("case", func(t *testing.T) {
        t.Parallel()
        // ...
    })
}
```

**nonamedreturns** — remove named return values.

**lll** — line too long (limit: 120 chars). Break the line or extract a variable.

**modernize** — use modern Go idioms (e.g. `min()`, `max()`, `slices.Contains()`).

**err113** — don't create errors inline: `errors.New("msg")` inside return. Define sentinel errors as package-level vars.

## Nolint policy

Use `//nolint` only for genuine false positives. Must include explanation:
```go
//nolint:gosec // path built from XDG config dir, not user input
```

**Never suppress a real bug** with nolint. Fix it instead.

`nolintlint` is enabled — every nolint directive must have a reason, or it will fail CI.
