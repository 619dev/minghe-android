#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
sdk="${LINPHONE_SOURCE_DIR:-$root/.sdk/source}"
version=5.4.129
revision=8121d3ba2e0e00a3a900d2efc5f2d3e2e235d665
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK}"
export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-$ANDROID_HOME/ndk/27.0.12077973}"

mkdir -p "$root/.sdk"
if [[ ! -d "$sdk/.git" ]]; then
    git clone --depth 1 --branch "$version" \
        https://github.com/BelledonneCommunications/linphone-sdk.git "$sdk"
fi
cd "$sdk"
[[ "$(git rev-parse HEAD)" == "$revision" ]]
git submodule update --init --recursive --jobs "${JOBS:-4}"

python3 "$root/scripts/strip-linphone-push.py" "$sdk"

cmake --preset=android-sdk -B "$root/.sdk/build" \
    -DCMAKE_BUILD_TYPE=Release \
    -DLINPHONESDK_ANDROID_ARCHS="${ANDROID_ARCHS:-arm64,armv7,x86_64}" \
    -DENABLE_GPL_THIRD_PARTIES=ON -DENABLE_NON_FREE_FEATURES=OFF \
    -DENABLE_VIDEO=OFF -DENABLE_OBOE=OFF -DENABLE_WEBRTC=OFF \
    -DENABLE_SRTP=ON -DENABLE_ZRTP=ON -DENABLE_GOCLEAR=OFF
cmake --build "$root/.sdk/build" --parallel "${JOBS:-4}"

repository="$root/.sdk/build/maven_repository"
artifact="$repository/org/linphone/linphone-sdk-android/$version/linphone-sdk-android-$version.aar"
python3 - "$artifact" <<'PY'
import io
import sys
import zipfile

with zipfile.ZipFile(sys.argv[1]) as aar:
    with zipfile.ZipFile(io.BytesIO(aar.read("classes.jar"))) as classes:
        for name in classes.namelist():
            if name.endswith(".class") and any(
                match in classes.read(name)
                for match in (b"com/google/firebase", b"com/google/android/gms",
                              b"com.google.firebase", b"com.google.android.gms")
            ):
                raise SystemExit(f"Unexpected Google SDK reference in {name}")
PY
mkdir -p "$root/.sdk/maven_repository"
cp -R "$repository/." "$root/.sdk/maven_repository/"
