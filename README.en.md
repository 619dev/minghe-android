# Minghe for Android

[简体中文](README.md) · [English](README.en.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Русский](README.ru.md) · [Français](README.fr.md) · [Español](README.es.md) · [Deutsch](README.de.md)

Minghe is an Android SIP voice client for self-hosted Minghe servers. The React and TypeScript interface uses Capacitor to call the native Linphone service. It follows the system language by default and supports eight languages.

## Connect and call

1. In **Account**, enter the server IP address or hostname, SIP extension and password. Tap **Go online**. The defaults are TLS port 5061, SHA-256 Digest and SDES-SRTP. Enable MD5 only if your server requires it. Select ZRTP in **Advanced settings** when your server uses ZRTP relay.
2. Enter the peer extension on the dial pad. Call history shows incoming and outgoing calls, connection status and time. Tap an entry to fill in the extension. Tap an incoming call notification to answer in the app, or decline from the notification.
3. In ZRTP mode, compare the SAS code aloud with your peer. Confirm only if both codes match. SDES-SRTP uses server-side media decryption and re-encryption.

When you first connect to a server with a self-signed certificate, Minghe retrieves the certificate and displays its SHA-256 fingerprint. Compare it with the fingerprint obtained from your administrator before accepting. You can also paste the PEM from a trusted copy of `server.crt` in **Advanced settings**. If the certificate rotates, clear the previous one and verify the replacement. For Docker Compose deployments, run the first command on the server. Save its public PEM as `server.crt` before running the second command.

```sh
docker compose exec -T minghe cat /app/certs/server.crt
openssl x509 -in server.crt -noout -fingerprint -sha256
```

**Save password** is enabled by default. Android Keystore protects the stored password, and the app can reuse it when you reopen it or restart the device. If disabled, enter the password again after the process stops. Allow notifications so the foreground SIP service can receive calls with the app closed. **Go offline** stops registration. Android battery restrictions may interrupt the connection.

## Appearance and screen layout

Under **About → Appearance**, choose Follow system, Light, or Dark. Minghe remembers the choice and updates the Android system bars. The phone screen stacks the dial pad and call history in narrow windows and places them side by side in wider windows. The layout respects display cutouts and system gesture areas.

## Build from source

Install JDK 21, Node.js 22.12 or a newer supported LTS, Android SDK Platforms 31 and 36, Build Tools 36.0.0 and NDK 27.0.12077973. The Linphone build also needs Git, CMake, Make, Python 3, Pystache, Doxygen, Meson, NASM, Yasm and pkg-config. Set `ANDROID_HOME` and accept the SDK licenses.

```sh
npm ci
npm run build
scripts/build-linphone.sh
cd android
./gradlew assembleDebug
```

The debug APK is at `android/app/build/outputs/apk/debug/app-debug.apk`. Run `./gradlew :app:assembleRelease` for an unsigned release. The SDK script builds pinned Linphone 5.4.129 from source, removes Firebase integration and stages the result locally. By default it builds arm64, armv7 and x86_64. Use `ANDROID_ARCHS=arm64` for an arm64-only build.

## Release verification

Verify SIP registration, incoming calls while locked, two-way audio, both encryption modes and SAS verification on two devices. Check network changes, reboot recovery and explicit sign-out. See the linked F-Droid guide for the source-build recipe and submission steps.

Author 619dev · [minghe.mobile](https://minghe.mobile) · v1.0.3 · [GNU AGPL v3.0](LICENSE) · [Changelog](CHANGELOG.md) · [F-Droid](fdroid/README.md)
