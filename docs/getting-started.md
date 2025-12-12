# Getting Started

## Requirements

**Server (Linux)**
- Any Linux distribution with systemd
- Desktop environment with D-Bus notifications (GNOME, KDE, XFCE, i3, Sway, etc.)

**App (Android)**
- Android 14 or higher

**Network**
- Both devices on the same local network (WiFi)

## Step 1: Install the Server

**Fedora / RHEL 10 / CentOS Stream 10:**
```bash
sudo dnf copr enable alessandrolattao/lanotifica
sudo dnf install lanotifica
```

**Ubuntu / Debian:**
```bash
curl -sLO $(curl -s https://api.github.com/repos/alessandrolattao/lanotifica/releases/latest | grep -o 'https://[^"]*\.deb') && sudo dpkg -i lanotifica_*.deb && rm lanotifica_*.deb
```

## Step 2: Start the Server

```
systemctl --user enable --now lanotifica
```

This starts the server and enables it to run automatically on login.

## Step 3: Install the App

Download from [Google Play](https://play.google.com/store/apps/details?id=com.alessandrolattao.lanotifica) or build from source.

## Step 4: Connect

1. Open `https://localhost:19420` in your browser
2. Accept the self-signed certificate warning
3. Open the LaNotifica app on your phone
4. Grant notification access when prompted
5. Tap "Scan QR Code" and scan the code shown on your desktop
6. Enable the forwarding toggle

Your phone notifications will now appear on your desktop.
