---
name: project-overview
description: Use when starting work on LaNotifica without context, or when asked what the project does, how components interact, or where a feature should live.
---

# LaNotifica — Project Overview

## What it does

LaNotifica forwards Android push notifications to a Linux desktop via D-Bus.

```
Android app → HTTPS POST /notification → Go server → D-Bus → desktop notification
```

Pairing: user opens `https://lanotifica.local:<port>/` in browser (PIN-protected since v1.1.0), scans QR code with Android app. QR encodes `<bearer-token>|<cert-fingerprint>`. Server is discovered via mDNS (`lanotifica.local`).

## Components

| Component | Path | Purpose |
|-----------|------|---------|
| Go server | `server/` | HTTPS API, D-Bus, mDNS, icon cache |
| Android app | `app/` | Listens for notifications, sends to server |
| Packaging | `packaging/` | RPM spec, DEB control, systemd unit |

## Go server internals

```
cmd/lanotifica/main.go          ← entrypoint, wires everything
internal/
  cert/      — TLS self-signed cert generation + fingerprint
  config/    — JSON config at ~/.config/lanotifica/config.json (XDG)
  handler/   — HTTP handlers: auth middleware, /notification, /health, /
  icon/      — Play Store icon fetcher + file cache
  mdns/      — mDNS registration (lanotifica._lanotifica._tcp.local)
  notification/ — D-Bus send/dismiss via go-notify
```

## Android app internals

MVVM architecture:
- `service/` — `NotificationListenerService` catches all Android notifications
- `network/` — Retrofit HTTPS client, certificate pinning via fingerprint
- `data/` — DataStore + Tink for encrypted storage of server URL + token
- `ui/` — Compose screens (pairing QR scan, settings, status)
- `di/` — Hilt dependency injection

## Key design decisions

- TLS is self-signed; Android pins by SHA-256 fingerprint (from QR), not CA
- mDNS uses UDP dial trick to detect LAN IP (avoids loopback advertising)
- Icons fetched from Play Store page, cached at `~/.cache/lanotifica/icons/`
- PIN (bcrypt) protects the web page; Bearer token protects the API
- Config is XDG: `~/.config/lanotifica/config.json` (mode 0600)
