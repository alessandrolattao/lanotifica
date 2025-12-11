# Getting Started

## Requirements

**Server (Linux)**
- Any Linux distribution with systemd
- Desktop environment with D-Bus notifications (GNOME, KDE, XFCE, i3, Sway, etc.)

**App (Android)**
- Android 14 or higher

**Network**
- Both devices on the same local network (WiFi)

## Installation

### Server

**Fedora / RHEL:**
```
curl -sLO $(curl -s https://api.github.com/repos/alessandrolattao/lanotifica/releases/latest | grep -o 'https://[^"]*\.rpm') && sudo dnf install -y lanotifica*.rpm && rm lanotifica*.rpm
```

**Ubuntu / Debian:**
```
curl -sLO $(curl -s https://api.github.com/repos/alessandrolattao/lanotifica/releases/latest | grep -o 'https://[^"]*\.deb') && sudo dpkg -i lanotifica_*.deb && rm lanotifica_*.deb
```

After installation, start the service and open the web interface at `https://localhost:19420`.

### App

Download from [Google Play](https://play.google.com/store/apps/details?id=com.alessandrolattao.lanotifica) or build from source.

## Setup

1. Start the server on your Linux machine
2. Open `https://localhost:19420` in your browser
3. Accept the self-signed certificate warning
4. Open the LaNotifica app on your phone
5. Grant notification access when prompted
6. Tap "Scan QR Code" and scan the code shown on your desktop
7. Enable the forwarding toggle

Your phone notifications will now appear on your desktop.

## Autostart

The server runs as a user service. To start it and enable it at login, run:

```
systemctl --user enable --now lanotifica
```
