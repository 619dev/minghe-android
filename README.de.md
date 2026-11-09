# Minghe für Android

[简体中文](README.md) · [English](README.en.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Русский](README.ru.md) · [Français](README.fr.md) · [Español](README.es.md) · [Deutsch](README.de.md)

Minghe ist ein SIP-Telefonieclient für selbst betriebene Minghe-Server. Die Oberfläche aus React und TypeScript verwendet Capacitor zur Anbindung des nativen Linphone-Diensts. Standardmäßig folgt die App der Systemsprache und unterstützt acht Sprachen.

## Verbinden und telefonieren

1. Gib unter **Konto** die IP-Adresse oder den Hostnamen, deine Nebenstelle und das SIP-Passwort ein. Gehe online. Standardwerte sind TLS 5061, SHA-256 Digest und SDES-SRTP. Erlaube MD5 nur, wenn der Server es verlangt. Wähle in den erweiterten Einstellungen ZRTP, wenn der Server ZRTP weiterleitet.
2. Wähle die Nebenstelle über das Tastenfeld. Die Anrufliste zeigt Richtung, Uhrzeit und Ergebnis. Tippe auf einen Eintrag, um die Nummer zu übernehmen. Öffne die Benachrichtigung, um in der App abzuheben, oder lehne direkt dort ab.
3. Vergleicht bei einem ZRTP-Anruf den SAS-Code mündlich. Bei SDES-SRTP entschlüsselt und verschlüsselt der Server den Audiostrom erneut.

Beim ersten Verbindungsversuch zu einem Server mit selbstsigniertem Zertifikat ruft die App das Zertifikat ab und zeigt den SHA-256-Fingerabdruck an. Vergleiche ihn vor der Bestätigung mit dem Wert vom Administrator. Alternativ kannst du in den erweiterten Einstellungen ein vertrauenswürdiges PEM einfügen. Entferne bei einem Zertifikatswechsel das alte Zertifikat und prüfe das neue. Bei Docker Compose führst du den ersten Befehl auf dem Server aus und speicherst das öffentliche PEM für den zweiten Befehl als `server.crt`.

```sh
docker compose exec -T minghe cat /app/certs/server.crt
openssl x509 -in server.crt -noout -fingerprint -sha256
```

Die Passwortspeicherung ist standardmäßig aktiv und durch Android Keystore geschützt. Bei deaktivierter Speicherung musst du das Passwort nach Prozessende erneut eingeben. Erlaube Benachrichtigungen, damit der Vordergrunddienst bei geschlossener Oberfläche Anrufe empfängt. Offline gehen beendet die SIP-Registrierung. Android-Energiesparregeln können die Verbindung unterbrechen.

## Darstellung und Bildschirmaufteilung

Wähle unter **Über → Darstellung** zwischen Systemeinstellung, Hell und Dunkel. Minghe speichert die Auswahl und passt die Android-Systemleisten an. In schmalen Fenstern stehen Tastenfeld und Anrufliste untereinander, in breiten nebeneinander. Die Oberfläche berücksichtigt Displayaussparungen und Systemgestenbereiche.

## Aus dem Quellcode bauen

Installiere JDK 21, Node.js 22.12 oder eine neuere unterstützte LTS-Version, Android SDK Platform 31 und 36, Build Tools 36.0.0 und NDK 27.0.12077973. Linphone benötigt außerdem Git, CMake, Make, Python 3, Pystache, Doxygen, Meson, NASM, Yasm und pkg-config. Setze `ANDROID_HOME` und akzeptiere die SDK-Lizenzen.

```sh
npm ci
npm run build
scripts/build-linphone.sh
cd android
./gradlew assembleDebug
```

Das Debug-APK liegt unter `android/app/build/outputs/apk/debug/app-debug.apk`. Ein unsigniertes Release entsteht mit `./gradlew :app:assembleRelease`. Das Skript baut Linphone 5.4.129 aus dem Quellcode und entfernt Firebase. Standardmäßig werden arm64, armv7 und x86_64 gebaut. Mit `ANDROID_ARCHS=arm64` wird nur arm64 gebaut.

## Release prüfen

Prüfe auf zwei Geräten die SIP-Registrierung, Anrufe bei gesperrtem Bildschirm, Audio in beide Richtungen, beide Verschlüsselungsmodi und die SAS-Prüfung. Teste Netzwerkwechsel, Wiederverbindung nach einem Neustart und das manuelle Abmelden.

Autor 619dev · [minghe.mobile](https://minghe.mobile) · v1.0.3 · [GNU AGPL v3.0](LICENSE) · [Changelog](CHANGELOG.md) · [F-Droid](fdroid/README.md)
