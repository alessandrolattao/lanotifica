---
name: go-server-structure
description: Use when adding or modifying Go server code in LaNotifica, looking for where a feature belongs, or unsure which package to touch.
---

# Go Server Structure

## Package map

```
server/
  cmd/lanotifica/main.go      — entrypoint: wires config, cert, mDNS, HTTP server, graceful shutdown
  internal/
    cert/       — TLS self-signed cert generation, SHA-256 fingerprint
    config/     — Load/Save JSON config (~/.config/lanotifica/config.json, XDG)
    handler/    — HTTP: AuthMiddleware, Notification, Health, HomeHandler, FaviconHandler
    icon/       — Play Store icon fetch + file cache (~/.cache/lanotifica/icons/)
    mdns/       — mDNS registration via hashicorp/mdns
    notification/ — D-Bus send/dismiss via TheCreeper/go-notify
```

## Where to add things

| What | Where |
|------|-------|
| New HTTP endpoint | `handler/` — add handler func + register in `main.go` |
| New config field | `config/config.go` Config struct + update `DefaultConfig()` |
| New background service | new `internal/<name>/` package, start in `main.go` |
| New notification behavior | `notification/notification.go` |

## Code conventions

**Errors** — always wrap with context:
```go
if err != nil {
    return fmt.Errorf("loading icon for %s: %w", packageName, err)
}
```

**Defers** — discard close errors explicitly:
```go
defer func() { _ = f.Close() }()
```

**Tests** — every test and subtest calls `t.Parallel()`:
```go
func TestFoo(t *testing.T) {
    t.Parallel()
    for _, tc := range cases {
        t.Run(tc.name, func(t *testing.T) {
            t.Parallel()
            // ...
        })
    }
}
```

**Nolint** — only for genuine false positives, always with explanation:
```go
//nolint:gosec // path is constructed from XDG config dir, not user input
```

**No nolint without explanation** — nolintlint is enabled and enforced.

## Linting

Config: `server/.golangci.yml` (golangci-lint v2, strict).

Key enabled linters: `wrapcheck`, `modernize`, `exhaustive`, `lll` (120 chars), `tparallel`, `nonamedreturns`, `err113`, `revive` (cognitive complexity ≤15), `gosec`.

Run: `golangci-lint run ./...` from `server/`.

## HTTP auth pattern

All protected endpoints go through `AuthMiddleware`:
```go
mux.HandleFunc("/endpoint", handler.AuthMiddleware(cfg.Secret, handler.MyHandler))
```

Auth uses `crypto/subtle.ConstantTimeCompare` — do not use `==` for token comparison.
