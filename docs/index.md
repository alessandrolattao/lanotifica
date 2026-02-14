---
layout: default
title: LaNotifica - Forward Android Notifications to Linux Desktop
description: >-
  Mirror your Android notifications to your Linux desktop over your local network.
  Encrypted with TLS, auto-discovery via mDNS, zero configuration.
  Works with GNOME, KDE, XFCE, i3, Sway. Open source (AGPL-3.0).
---

# LaNotifica

Forward your Android notifications to your Linux desktop.

## Overview

LaNotifica is a simple tool that sends your phone notifications directly to your Linux desktop. WhatsApp, Telegram, calls — everything. All communication stays on your local network — no cloud, no accounts, no tracking.

## How It Works

```
┌──────────────┐      HTTPS/TLS       ┌──────────────┐
│              │    Local Network     │              │
│   Android    │ ──────────────────►  │    Linux     │
│    Phone     │    mDNS Discovery    │   Desktop    │
│              │                      │              │
└──────────────┘                      └──────────────┘
```

1. Install the server on your Linux machine
2. Open the app on your Android phone
3. Scan the QR code shown by the server
4. Done — notifications appear on your desktop

The server and app find each other automatically using mDNS. All communication is encrypted with TLS.

## Features

| | Feature | Description |
|---|---|---|
| 🔒 | **Zero cloud** | Everything stays on your local network |
| 🔐 | **Encrypted** | TLS with auto-generated certificates |
| ✨ | **Zero config** | mDNS auto-discovery, no IP addresses to type |
| 🔋 | **Battery friendly** | Minimal impact on your phone |
| 🐧 | **Works everywhere** | GNOME, KDE, XFCE, i3, Sway... |

## Quick Start

### Server (Linux)

**Fedora / RHEL / CentOS Stream:**
```bash
sudo dnf copr enable alessandrolattao/lanotifica
sudo dnf install lanotifica
```

**Ubuntu / Debian:**
```bash
curl -sLO $(curl -s https://api.github.com/repos/alessandrolattao/lanotifica/releases/latest | grep -o 'https://[^"]*\.deb') && sudo dpkg -i lanotifica_*.deb && rm lanotifica_*.deb
```

**Start the server:**
```bash
systemctl --user enable --now lanotifica
```

### App (Android)

[Download on Google Play](https://play.google.com/store/apps/details?id=com.alessandrolattao.lanotifica) or build from source.

## Documentation

- [Getting Started](getting-started.md) — Installation and setup guide
- [Architecture](architecture.md) — How LaNotifica works under the hood
- [Security](security.md) — Encryption, authentication, and privacy details
- [Troubleshooting](troubleshooting.md) — Common issues and solutions
- [Privacy Policy](privacy.md)

## Links

- [GitHub Repository](https://github.com/alessandrolattao/lanotifica)
- [Download on Google Play](https://play.google.com/store/apps/details?id=com.alessandrolattao.lanotifica)
