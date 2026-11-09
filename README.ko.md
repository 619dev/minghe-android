# 밍허 Android

[简体中文](README.md) · [English](README.en.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Русский](README.ru.md) · [Français](README.fr.md) · [Español](README.es.md) · [Deutsch](README.de.md)

밍허는 자체 호스팅 Minghe 서버를 위한 Android SIP 음성 클라이언트입니다. React와 TypeScript 인터페이스는 Capacitor를 통해 네이티브 Linphone 서비스를 사용합니다. 기본값은 시스템 언어이며 8개 언어를 지원합니다.

## 연결 및 통화

1. **계정**에서 서버 IP 또는 호스트 이름, 내선 번호, SIP 비밀번호를 입력하고 온라인으로 전환합니다. 기본값은 TLS 5061, SHA-256 Digest, SDES-SRTP입니다. 서버에서 요구할 때만 MD5를 허용하고, ZRTP 중계 서버에서는 고급 설정에서 ZRTP를 선택합니다.
2. 다이얼 패드로 상대방 내선을 입력합니다. 통화 기록에서 방향, 연결 여부, 시간을 확인하고 항목을 눌러 번호를 다시 입력할 수 있습니다. 수신 알림을 눌러 앱에서 받거나 알림에서 거절할 수 있습니다.
3. ZRTP 통화에서는 상대방과 SAS 코드를 소리 내어 비교합니다. SDES-SRTP에서는 서버가 미디어를 복호화한 뒤 다시 암호화합니다.

자체 서명 인증서를 사용하는 서버에 처음 연결하면 앱이 인증서를 가져와 SHA-256 지문을 표시합니다. 관리자에게 받은 지문과 일치할 때만 승인하세요. 고급 설정에 신뢰할 수 있는 PEM을 직접 붙여 넣을 수도 있습니다. 인증서가 갱신되면 기존 인증서를 지우고 새 지문을 확인합니다. Docker Compose 환경에서는 서버에서 첫 명령을 실행한 뒤 공개 PEM을 `server.crt`로 저장하고 두 번째 명령을 실행합니다.

```sh
docker compose exec -T minghe cat /app/certs/server.crt
openssl x509 -in server.crt -noout -fingerprint -sha256
```

비밀번호 저장은 기본으로 켜져 있으며 Android Keystore로 보호됩니다. 저장을 끄면 프로세스 종료 후 비밀번호를 다시 입력해야 합니다. 알림을 허용하면 앱 화면이 닫혀도 포그라운드 서비스가 전화를 수신합니다. 오프라인으로 전환하면 등록이 중지됩니다. Android 절전 정책으로 연결이 끊길 수 있습니다.

## 화면 모드 및 레이아웃

「앱 정보 → 화면 모드」에서 시스템 설정 따르기, 밝게, 어둡게를 선택할 수 있습니다. 선택이 저장되며 Android 시스템 표시줄에도 반영됩니다. 좁은 창에서는 다이얼 패드와 통화 기록을 위아래로, 넓은 창에서는 나란히 표시합니다. 화면 잘림 영역과 시스템 제스처 영역을 고려합니다.

## 소스에서 빌드

JDK 21, Node.js 22.12 이상 지원 LTS, Android SDK Platform 31과 36, Build Tools 36.0.0, NDK 27.0.12077973이 필요합니다. Linphone 빌드에는 Git, CMake, Make, Python 3, Pystache, six, Doxygen, Meson, NASM, Yasm, pkg-config도 필요합니다. `ANDROID_HOME`을 설정하고 SDK 라이선스에 동의하세요.

```sh
npm ci
npm run build
scripts/build-linphone.sh
cd android
./gradlew assembleDebug
```

디버그 APK는 `android/app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다. 서명하지 않은 릴리스는 `./gradlew :app:assembleRelease`로 빌드합니다. 스크립트는 Firebase를 제거한 Linphone 5.4.129를 소스에서 빌드합니다. 기본 대상은 arm64, armv7, x86_64이며 arm64만 빌드하려면 `ANDROID_ARCHS=arm64`를 설정하세요.

## 출시 검증

기기 두 대에서 SIP 등록, 잠금 화면 수신, 양방향 음성, 두 암호화 방식, SAS 확인을 테스트합니다. 네트워크 전환, 재부팅 후 재등록, 수동 오프라인 전환도 확인합니다.

제작자 619dev · [minghe.mobile](https://minghe.mobile) · v1.0.3 · [GNU AGPL v3.0](LICENSE) · [Changelog](CHANGELOG.md) · [F-Droid](fdroid/README.md)
