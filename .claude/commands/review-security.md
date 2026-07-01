---
name: review-security
description: Use when reviewing code for security issues, adding auth to new endpoints, or handling sensitive data like tokens, PINs, or file paths.
---

# Security Review

## Auth model

| Layer | Mechanism |
|-------|-----------|
| Web page (`/`) | PIN (bcrypt) + 30-day session cookie (`ln_session`, Secure+HttpOnly+SameSite=Strict) |
| API endpoints | Bearer token via `Authorization: Bearer <secret>` header |
| Token comparison | `crypto/subtle.ConstantTimeCompare` — never `==` |

**Every new HTTP endpoint** must go through `AuthMiddleware` unless it's explicitly public (only `/health` is public).

## Checklist for new endpoints

- [ ] Protected with `AuthMiddleware` (or explicitly justified if not)
- [ ] Input validated at the boundary (package name regex, URL prefix checks)
- [ ] Body size limited with `http.MaxBytesReader` before reading
- [ ] File paths built from controlled values (not user input directly)
- [ ] Errors wrapped but internal details not leaked in HTTP response
- [ ] No timing attack surface (use `subtle.ConstantTimeCompare` for secrets)

## Input validation patterns

**Package name** (Android): must match `^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$`

**Icon URL**: must start with `https://play-lh.googleusercontent.com/` — validate the prefix before fetching.

**File paths**: build from `filepath.Join(cacheDir, packageName)` where `packageName` is already validated. Do not use raw user input in paths.

## Body size limiting

```go
r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MB for JSON
r.Body = http.MaxBytesReader(w, r.Body, 1<<10) // 1 KB for forms
```

Set before any read (`json.Decode`, `r.ParseForm`, etc.).

## Sensitive data

- Bearer token and PIN hash live in `~/.config/lanotifica/config.json` (mode 0600)
- Never log tokens, PINs, or PIN hashes
- Never compare secrets with `==` — use `subtle.ConstantTimeCompare`
- Session cookies: `Secure`, `HttpOnly`, `SameSite: Strict`

## gosec false positives

Only suppress with an explanation:
```go
//nolint:gosec // path is built from XDG config dir + validated package name, not raw user input
```

Real issues (SQL injection, command injection, path traversal) must be fixed, not suppressed.
