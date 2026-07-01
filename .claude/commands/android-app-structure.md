---
name: android-app-structure
description: Use when adding or modifying Android app code in LaNotifica, deciding where a feature belongs, or understanding how the app communicates with the server.
---

# Android App Structure

## Architecture: MVVM + Hilt

```
app/app/src/main/java/com/alessandrolattao/lanotifica/
  service/    — NotificationListenerService: intercepts all Android notifications
  network/    — Retrofit HTTPS client, TLS cert pinning by SHA-256 fingerprint
  data/       — DataStore + Tink: encrypted storage for server URL + Bearer token
  ui/         — Compose screens (pairing/QR scan, settings, status)
  di/         — Hilt modules: provides network, data, repository instances
```

## Where to add things

| What | Where |
|------|-------|
| New screen | `ui/` — Composable + ViewModel |
| New API call to server | `network/` — Retrofit interface + data class |
| New persisted setting | `data/` — DataStore key + repository method |
| New notification filter/transform | `service/` |
| New DI binding | `di/` — Hilt module |

## How notifications flow

```
NotificationListenerService
  → filters by package/key
  → builds Request(appName, packageName, title, message, urgency, key)
  → POST /notification via Retrofit (HTTPS, Bearer token)
  → server responds 200 or error
```

## TLS / pairing

- Server uses self-signed cert; fingerprint is in the QR code
- Android pins by SHA-256 fingerprint stored in DataStore (Tink-encrypted)
- Server URL also stored encrypted in DataStore
- Pairing: scan QR → parse `<token>|<fingerprint>` → store both

## Build stack

| Tool | Version |
|------|---------|
| Gradle | 9.4.1 |
| AGP | 9.2.0 |
| Kotlin | 2.3.21 |
| compileSdk | 37 |
| minSdk | 34 |

Dependencies managed in `app/gradle/libs.versions.toml` (version catalog).

**Formatting:** Spotless — run `make lint-app` before pushing. Spotless enforces Kotlin style; CI will fail on unformatted code.

**Adding a dependency:** add version to `libs.versions.toml`, add library entry, reference with `libs.<alias>` in `build.gradle.kts`.
