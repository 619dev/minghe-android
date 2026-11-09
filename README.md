# 鸣鹤 Android

[简体中文](README.md) · [English](README.en.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Русский](README.ru.md) · [Français](README.fr.md) · [Español](README.es.md) · [Deutsch](README.de.md)

面向自托管鸣鹤 SIP 服务器的 Android 客户端。React 和 TypeScript 实现界面和八种语言。Capacitor 定义平台桥接，Android 的 SIP/TLS、音频和后台运行由原生 Linphone 服务处理。将来适配 iOS 时可以复用界面，并为 [SIP 桥接](src/sip.ts) 实现 iOS 端。

## 配置

1. 在「账号」输入与 TLS 证书匹配的服务器 IP 或域名、分机号及 SIP 密码，然后点「上线」。
2. 默认使用 TLS 5061、SHA-256 Digest 和 SDES-SRTP。服务器使用 ZRTP 中继时，在「高级设置」切换到 ZRTP。只有部署显式开启 MD5 Digest 时才勾选 MD5 兼容。
3. 「电话」里用拨号盘输入同一服务器上的对方分机号。通话结束后，记录会显示方向、时间和接通状态，点一条记录可回填分机号。来电时点通知打开接听界面，或直接在通知中拒接。ZRTP 接通后，双方口头核对 SAS 验证码，一致后在应用中确认。

应用首次连接自签名服务器时会自动获取证书并显示 SHA-256 指纹。请与管理员提供的指纹核对后确认。也可在「高级设置 → 服务器证书」粘贴可信来源的 PEM。证书更新后清除旧证书，再核对新指纹。Docker Compose 默认将证书保存在挂载到 `/app/certs` 的命名卷中。第一条命令在服务器上执行，将公开 PEM 保存为 `server.crt` 后执行第二条命令。

```sh
docker compose exec -T minghe cat /app/certs/server.crt
openssl x509 -in server.crt -noout -fingerprint -sha256
```

「保存密码」默认开启，密码经 Android Keystore 加密后保存。关闭后，下次上线不会保留密码。前台服务仍能在本次会话保持在线，设备重启或进程终止后需要重新输入密码。

语音模式按服务端配置选择。SDES-SRTP 由服务端终止并重新加密，ZRTP 由两端协商密钥。应用要求媒体加密。

允许通知后，前台服务维持 SIP/TLS 注册，并在应用界面退出后接收来电。已上线的账号会在设备重启解锁后尝试恢复注册。「下线」停止服务和重启恢复。Android 省电策略、系统强行停止和设备厂商的后台管理都可能中断连接。可通过「高级设置 → 电池优化设置」查看系统选项。

默认跟随系统语言，也可手动切换。支持简体中文、英文、日文、韩文、俄文、法文、西班牙文和德文。

## 外观与屏幕

在「关于 → 外观」选择跟随系统、亮色或暗色。应用会记住选择，并同步调整 Android 系统栏。电话页按窗口宽度布局。窄屏上下排列拨号盘和通话记录，宽屏并排显示，并为屏幕开孔和系统手势区留出空间。

## 从源码构建

使用 JDK 21、Node.js 22.12 或更新的受支持 LTS、Android SDK Platform 31 和 36、Build Tools 36.0.0、NDK r27.0.12077973。构建 Linphone 还需要 Git、CMake、Make、Python 3、Pystache、six、Doxygen、Meson、NASM、Yasm 和 pkg-config。请预先接受 Android SDK 许可证，并将 `ANDROID_HOME` 指向 SDK。

```sh
npm ci
npm run build
scripts/build-linphone.sh
cd android
./gradlew assembleDebug
```

调试 APK 路径为 `android/app/build/outputs/apk/debug/app-debug.apk`。未签名发行包使用 `./gradlew :app:assembleRelease`。默认 SDK 构建架构为 arm64、armv7 和 x86_64，可用 `ANDROID_ARCHS=arm64` 只构建 arm64。可用 `JOBS` 调整编译并发。

Gradle 从本地 `.sdk/maven_repository` 读取 Linphone。脚本核对 Linphone 5.4.129 的源码提交，移除 Firebase 集成，从源码编译，再检查生成的 Java 类是否引用 Firebase 或 Google Play 服务。Android SDK 和 AndroidX 用于系统 API 与构建。项目不依赖 GMS、Firebase、推送服务或分析 SDK。F-Droid 的商店文案位于 `fastlane/metadata/android`。正式提交前应在 F-Droid 构建环境复跑源码构建和扫描。

## 发布验收

- 在两台设备上分别验证 SDES-SRTP 和 ZRTP 的拨出、来电、锁屏响铃、挂断及双向语音。在 ZRTP 模式核对两端 SAS，并验证协商失败会断开。
- 测试网络切换、重启、锁屏和省电模式下的注册恢复，确认手动下线后不会在重启时重新上线。

应用代码使用 [GNU Affero General Public License v3.0](LICENSE)，SPDX 标识为 `AGPL-3.0-only`。Linphone 及其第三方组件使用各自的许可证。

作者 619dev · [minghe.mobile](https://minghe.mobile) · v1.0.3

[版本记录](CHANGELOG.md) · [F-Droid 提交说明](fdroid/README.md)
