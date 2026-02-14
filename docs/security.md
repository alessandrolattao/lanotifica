---
title: LaNotifica Security
description: LaNotifica security model — TLS 1.3 encryption, certificate pinning, token-based authentication, and local-only communication.
---

# Security

## Design Principles

LaNotifica is designed with privacy and security as core principles:

- **Local only** - All communication stays on your local network
- **No cloud** - No data is sent to external servers
- **No accounts** - No registration or login required
- **Encrypted** - All traffic is encrypted with TLS

## Authentication

Each server generates a unique authentication token on first startup. This token is shared with the app via QR code and is required for all API requests.

The token is stored securely on both the server and the app.

## Encryption

All communication between the app and server uses HTTPS with TLS 1.3.

The server generates a self-signed certificate on first startup. The certificate fingerprint is included in the QR code, allowing the app to verify it's talking to the correct server (certificate pinning).

## Certificate Pinning

The app stores the server's certificate fingerprint and verifies it on every connection. This prevents man-in-the-middle attacks even if an attacker has access to your local network.

## Permissions

The Android app requires:

- **Notification access** - To read and forward notifications
- **Camera** - To scan the QR code during setup
- **Local network** - To communicate with the server

No internet permission is required after the initial app download.
