---
name: new-feature
description: Use when implementing a new feature to decide where code goes, what patterns to follow, and what to verify before pushing.
---

# New Feature

## Decide where it lives

**Server-side** (Go): config, HTTP endpoints, notification behavior, icon caching, mDNS.
**Android-side**: UI screens, notification filtering/transforms, device-local settings.
**Both**: features that require Android to send new data AND server to process it.

When in doubt: Android-side for anything the user configures on the device; server-side for anything that affects how notifications reach the desktop.

## Server: adding a feature

1. Decide the package (see `go-server-structure` skill for the package map)
2. Add config field if needed: `config/config.go` → `Config` struct + `DefaultConfig()`
3. Add HTTP handler if needed: `handler/` → register in `main.go` with `AuthMiddleware` if protected
4. Wire in `main.go`: pass new deps to handlers via closure or constructor
5. Write tests with `t.Parallel()` on every test and subtest
6. Run `golangci-lint run ./...` before pushing

## Android: adding a feature

1. New screen: `ui/` — Composable + ViewModel
2. New persistent setting: `data/` — DataStore key + repository method
3. New API call: `network/` — Retrofit interface + data class
4. New notification transform: `service/NotificationListenerService`
5. New DI binding: `di/` — Hilt module
6. Run `./gradlew lint` before pushing

## Rules

- No features without tests on the server side
- No new HTTP endpoints without `AuthMiddleware` (unless it's `/health`)
- All config fields must have a sensible default in `DefaultConfig()`
- All errors must be wrapped with context (`fmt.Errorf("...: %w", err)`)
- No `nolint` without a comment explaining why it's a false positive
- Run lint + tests before pushing (see `lint-and-test` skill)
- If bumping version: follow `bump-version` skill

## Before pushing

```bash
cd server && golangci-lint run ./... && go test ./...
cd app && ./gradlew lint test
```
