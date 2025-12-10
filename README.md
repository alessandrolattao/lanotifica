<p align="center">
  <img src="assets/lanotifica.png" width="400" alt="LaNotifica">
</p>

<h1 align="center">LaNotifica</h1>

<p align="center">
  <strong>Forward Android notifications to your Linux desktop.</strong>
</p>

<p align="center">
  <a href="https://github.com/alessandrolattao/lanotifica/actions/workflows/ci.yml">
    <img src="https://github.com/alessandrolattao/lanotifica/actions/workflows/ci.yml/badge.svg" alt="CI">
  </a>
  <a href="https://play.google.com/store/apps/details?id=com.alessandrolattao.lanotifica">
    <img src="https://img.shields.io/badge/Google_Play-Download-green?style=for-the-badge&logo=google-play" alt="Google Play">
  </a>
  <img src="https://img.shields.io/badge/Android-14+-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 14+">
  <img src="https://img.shields.io/badge/Go-1.21+-00ADD8?style=for-the-badge&logo=go&logoColor=white" alt="Go">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="MIT License">
</p>

<p align="center">
  Never miss a message while working.<br>
  LaNotifica mirrors your phone notifications directly to your desktop —<br>
  WhatsApp, Telegram, calls, everything.
</p>

---

## How it works

```
┌──────────────┐      HTTPS/TLS       ┌──────────────┐
│              │    Local Network     │              │
│   Android    │ ──────────────────►  │    Linux     │
│    Phone     │    mDNS Discovery    │   Desktop    │
│              │                      │              │
└──────────────┘                      └──────────────┘
```

1. **Run the server** on your Linux machine
2. **Scan the QR code** with the Android app
3. **Done.** Notifications appear on your desktop

---

## Features

| | Feature | Description |
|---|---|---|
| 🔒 | **Zero cloud** | Everything stays on your local network |
| 🔐 | **Encrypted** | TLS with auto-generated certificates |
| ✨ | **Zero config** | mDNS auto-discovery, no IP addresses to type |
| 🔋 | **Battery friendly** | Minimal impact on your phone |
| 🐧 | **Works everywhere** | GNOME, KDE, XFCE, i3, Sway... |

---

## Quick Start

### Server (Linux)

```bash
go install github.com/alessandrolattao/lanotifica/cmd/lanotifica@latest
lanotifica
```

Open `https://localhost:8443` and scan the QR code.

### App (Android)

<a href="https://play.google.com/store/apps/details?id=com.alessandrolattao.lanotifica">
  <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="80">
</a>

Or build from source in `app/`.

---

## Requirements

| Component | Requirement |
|-----------|-------------|
| **Server** | Linux with D-Bus notifications (any modern desktop) |
| **App** | Android 14+ (API 34) |
| **Network** | Both devices on the same local network |

---

## Security

- **Token-based authentication** — Unique token generated on setup
- **Certificate pinning** — Fingerprint verified via QR code
- **End-to-end encryption** — All traffic over TLS
- **LAN only** — Never leaves your local network

---

## License

MIT
