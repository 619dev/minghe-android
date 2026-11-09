# Minghe para Android

[简体中文](README.md) · [English](README.en.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Русский](README.ru.md) · [Français](README.fr.md) · [Español](README.es.md) · [Deutsch](README.de.md)

Minghe es un cliente de llamadas SIP para servidores Minghe autohospedados. La interfaz en React y TypeScript usa Capacitor para comunicarse con el servicio nativo de Linphone. Sigue el idioma del sistema de forma predeterminada y ofrece ocho idiomas.

## Conexión y llamadas

1. En **Cuenta**, introduce la IP o el nombre del servidor, la extensión y la contraseña SIP. Conéctate. Los valores predeterminados son TLS 5061, Digest SHA-256 y SDES-SRTP. Permite MD5 solo si el servidor lo exige. Selecciona ZRTP en los ajustes avanzados si tu servidor usa retransmisión ZRTP.
2. Marca la extensión en el teclado. El historial muestra la dirección, la hora y el resultado de cada llamada. Toca una entrada para rellenar el número. Abre la notificación de llamada para contestar en la aplicación o recházala desde la notificación.
3. En una llamada ZRTP, compara el código SAS en voz alta. En SDES-SRTP, el servidor descifra y vuelve a cifrar el audio.

Al conectarse por primera vez a un servidor con certificado autofirmado, la aplicación obtiene el certificado y muestra la huella SHA-256. Compárala con la facilitada por el administrador antes de aceptar. También puedes pegar un PEM de confianza en los ajustes avanzados. Si se renueva el certificado, borra el anterior y verifica el nuevo. Con Docker Compose, ejecuta el primer comando en el servidor y guarda el PEM público como `server.crt` para el segundo.

```sh
docker compose exec -T minghe cat /app/certs/server.crt
openssl x509 -in server.crt -noout -fingerprint -sha256
```

Guardar la contraseña está habilitado de forma predeterminada y usa Android Keystore. Al desactivarlo, vuelve a introducir la contraseña cuando se detenga el proceso. Permite las notificaciones para recibir llamadas con la interfaz cerrada. Desconectarte detiene el registro SIP. Las restricciones de batería de Android pueden interrumpir la conexión.

## Apariencia y diseño de pantalla

En **Acerca de → Apariencia**, elige el tema del sistema, claro u oscuro. Minghe guarda la selección y adapta las barras del sistema Android. En ventanas estrechas, el teclado se muestra sobre el historial. En ventanas anchas, aparecen en paralelo. La interfaz respeta los recortes de pantalla y las zonas de gestos del sistema.

## Compilar desde el código fuente

Instala JDK 21, Node.js 22.12 o una LTS compatible posterior, Android SDK Platform 31 y 36, Build Tools 36.0.0 y NDK 27.0.12077973. Linphone también requiere Git, CMake, Make, Python 3, Pystache, six, Doxygen, Meson, NASM, Yasm y pkg-config. Configura `ANDROID_HOME` y acepta las licencias del SDK.

```sh
npm ci
npm run build
scripts/build-linphone.sh
cd android
./gradlew assembleDebug
```

El APK de depuración se genera en `android/app/build/outputs/apk/debug/app-debug.apk`. Para una versión sin firmar, ejecuta `./gradlew :app:assembleRelease`. El script compila Linphone 5.4.129 desde el código fuente tras retirar Firebase. Compila arm64, armv7 y x86_64 de forma predeterminada. Usa `ANDROID_ARCHS=arm64` para compilar solo arm64.

## Verificación de la versión

En dos dispositivos, verifica el registro SIP, las llamadas con la pantalla bloqueada, el audio bidireccional, ambos modos de cifrado y la comprobación del SAS. Prueba el cambio de red, la recuperación tras reiniciar y la desconexión manual.

Autor 619dev · [minghe.mobile](https://minghe.mobile) · v1.0.3 · [GNU AGPL v3.0](LICENSE) · [Changelog](CHANGELOG.md) · [F-Droid](fdroid/README.md)
