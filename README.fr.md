# Minghe pour Android

[简体中文](README.md) · [English](README.en.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Русский](README.ru.md) · [Français](README.fr.md) · [Español](README.es.md) · [Deutsch](README.de.md)

Minghe est un client d’appels SIP pour les serveurs Minghe auto-hébergés. L’interface React et TypeScript communique avec le service natif Linphone via Capacitor. L’application suit par défaut la langue du système et propose huit langues.

## Connexion et appels

1. Dans **Compte**, saisissez l’adresse IP ou le nom du serveur, le poste et le mot de passe SIP, puis connectez-vous. Les valeurs par défaut sont TLS 5061, Digest SHA-256 et SDES-SRTP. Autorisez MD5 seulement si le serveur l’exige. Sélectionnez ZRTP dans les paramètres avancés pour un relais ZRTP.
2. Composez le poste sur le clavier. Le journal indique le sens, l’heure et le résultat des appels. Touchez une entrée pour reprendre le numéro. Touchez la notification d’appel pour répondre dans l’application, ou refusez depuis la notification.
3. Pour un appel ZRTP, comparez oralement le code SAS. Avec SDES-SRTP, le serveur déchiffre puis rechiffre les flux audio.

À la première connexion à un serveur doté d’un certificat autosigné, l’application récupère le certificat et affiche son empreinte SHA-256. Comparez-la avec celle fournie par votre administrateur avant de l’accepter. Vous pouvez aussi coller un PEM de confiance dans les paramètres avancés. Lors du renouvellement, retirez l’ancien certificat et vérifiez le nouveau. Avec Docker Compose, lancez la première commande sur le serveur puis enregistrez le PEM public sous `server.crt` pour la seconde.

```sh
docker compose exec -T minghe cat /app/certs/server.crt
openssl x509 -in server.crt -noout -fingerprint -sha256
```

L’enregistrement du mot de passe est activé par défaut et protégé par Android Keystore. Si vous le désactivez, ressaisissez le mot de passe après l’arrêt du processus. Autorisez les notifications pour recevoir les appels lorsque l’interface est fermée. La déconnexion arrête l’inscription SIP. Les restrictions de batterie d’Android peuvent interrompre la connexion.

## Apparence et disposition

Dans **À propos → Apparence**, choisissez le thème système, clair ou sombre. Minghe mémorise ce choix et adapte les barres système Android. Dans une fenêtre étroite, le clavier s’affiche au-dessus du journal d’appels. Dans une fenêtre large, ils s’affichent côte à côte. L’interface tient compte des découpes de l’écran et des zones de gestes système.

## Compilation depuis les sources

Installez JDK 21, Node.js 22.12 ou une LTS prise en charge plus récente, les plateformes Android SDK 31 et 36, Build Tools 36.0.0 et NDK 27.0.12077973. Linphone nécessite aussi Git, CMake, Make, Python 3, Pystache, six, Doxygen, Meson, NASM, Yasm et pkg-config. Définissez `ANDROID_HOME` et acceptez les licences du SDK.

```sh
npm ci
npm run build
scripts/build-linphone.sh
cd android
./gradlew assembleDebug
```

L’APK de débogage se trouve dans `android/app/build/outputs/apk/debug/app-debug.apk`. Lancez `./gradlew :app:assembleRelease` pour une version non signée. Le script compile Linphone 5.4.129 depuis les sources après retrait de Firebase. Les architectures par défaut sont arm64, armv7 et x86_64. Utilisez `ANDROID_ARCHS=arm64` pour cibler uniquement arm64.

## Validation de la version

Sur deux appareils, vérifiez l’inscription SIP, les appels avec écran verrouillé, l’audio bidirectionnel, les deux modes de chiffrement et la comparaison du SAS. Testez le changement de réseau, la reprise après redémarrage et la déconnexion manuelle.

Auteur 619dev · [minghe.mobile](https://minghe.mobile) · v1.0.3 · [GNU AGPL v3.0](LICENSE) · [Changelog](CHANGELOG.md) · [F-Droid](fdroid/README.md)
