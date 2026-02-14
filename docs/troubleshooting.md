---
title: LaNotifica Troubleshooting
description: Fix common issues with LaNotifica — server not starting, notifications not appearing, mDNS discovery problems, and Android app connection issues.
---

# Troubleshooting

## Server Issues

**Server won't start**
- Check if the service is running with your init system
- Look at the logs for error messages
- Make sure port 19420 is not in use by another application

**QR code page doesn't load**
- Verify the server is running
- Try accessing `https://localhost:19420` directly
- Accept the self-signed certificate warning in your browser

**Notifications don't appear on desktop**
- Check if your desktop environment supports D-Bus notifications
- Try sending a test notification from another application

## App Issues

**Can't scan QR code**
- Make sure camera permission is granted
- Ensure good lighting and steady hands
- Clean your camera lens

**App doesn't connect to server**
- Verify both devices are on the same network
- Check if your router blocks multicast traffic (mDNS)
- Try restarting both the server and the app

**Notifications not forwarded**
- Ensure notification access is granted in Android settings
- Check that the forwarding toggle is enabled in the app
- Verify the server is shown as "Connected" in the app

## Network Issues

**mDNS discovery fails**
- Some corporate networks block multicast traffic
- Guest networks often isolate devices from each other
- Try using a home network or mobile hotspot

**Connection drops frequently**
- Check your WiFi signal strength
- The server uses a health check to maintain the connection
- The app will automatically reconnect when the server is available
