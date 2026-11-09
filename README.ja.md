# 鳴鶴 Android

[简体中文](README.md) · [English](README.en.md) · [日本語](README.ja.md) · [한국어](README.ko.md) · [Русский](README.ru.md) · [Français](README.fr.md) · [Español](README.es.md) · [Deutsch](README.de.md)

鳴鶴は、自分で運用する Minghe サーバー向けの Android SIP 音声クライアントです。React と TypeScript の画面は Capacitor を通じて Linphone のネイティブサービスを呼び出します。既定ではシステム言語に従い、8 言語をサポートします。

## 接続と通話

1. **アカウント**でサーバーの IP またはホスト名、内線番号、SIP パスワードを入力してオンラインにします。既定値は TLS 5061、SHA-256 Digest、SDES-SRTP です。サーバーが要求する場合だけ MD5 を許可し、ZRTP 中継を使う場合は詳細設定で ZRTP を選びます。
2. ダイヤルパッドで相手の内線番号を入力します。履歴には発着信、接続状態、時刻が表示され、項目をタップすると番号を再入力できます。着信通知からアプリを開いて応答するか、通知から拒否できます。
3. ZRTP 通話では SAS コードを相手と声に出して照合します。SDES-SRTP ではサーバーがメディアを復号して再暗号化します。

自己署名証明書を使うサーバーへの初回接続時に、アプリは証明書を取得し、SHA-256 フィンガープリントを表示します。管理者から取得した値と照合してから承認してください。詳細設定で信頼できる PEM を貼り付けることもできます。証明書を更新した場合は古い証明書を消去し、新しいものを確認します。Docker Compose では最初のコマンドをサーバーで実行し、公開 PEM を `server.crt` として保存してから 2 番目のコマンドを実行します。

```sh
docker compose exec -T minghe cat /app/certs/server.crt
openssl x509 -in server.crt -noout -fingerprint -sha256
```

パスワード保存は既定で有効で、Android Keystore により保護されます。無効にするとプロセスの終了後に再入力が必要です。通知を許可すると画面を閉じてもフォアグラウンドサービスが着信を待ち受けます。オフラインにすると登録が停止します。省電力機能によって接続が切れることがあります。

## 外観と画面レイアウト

「アプリ情報 → 外観」でシステムに従う、ライト、ダークを選べます。選択は保存され、Android のシステムバーにも反映されます。狭いウィンドウではダイヤルパッドと履歴を縦に並べ、広いウィンドウでは横に並べます。画面の切り欠きとシステムジェスチャー領域を避けて配置します。

## ソースからビルド

JDK 21、Node.js 22.12 以降のサポート対象 LTS、Android SDK Platform 31 と 36、Build Tools 36.0.0、NDK 27.0.12077973 を用意します。Linphone には Git、CMake、Make、Python 3、Pystache、Doxygen、Meson、NASM、Yasm、pkg-config も必要です。`ANDROID_HOME` を設定し、SDK ライセンスに同意します。

```sh
npm ci
npm run build
scripts/build-linphone.sh
cd android
./gradlew assembleDebug
```

デバッグ APK は `android/app/build/outputs/apk/debug/app-debug.apk` に生成されます。署名なしのリリースには `./gradlew :app:assembleRelease` を使います。スクリプトは Firebase を除いた Linphone 5.4.129 をソースからビルドします。対象は既定で arm64、armv7、x86_64 です。arm64 のみの場合は `ANDROID_ARCHS=arm64` を設定します。

## リリース前の確認

2 台の端末で SIP 登録、ロック中の着信、双方向音声、両暗号化方式、SAS の照合を確認します。ネットワーク切替、再起動後の復帰、手動でのオフライン切替も確認します。

作者 619dev · [minghe.mobile](https://minghe.mobile) · v1.0.3 · [GNU AGPL v3.0](LICENSE) · [Changelog](CHANGELOG.md) · [F-Droid](fdroid/README.md)
