"""Remove the optional Android push integration from the pinned SDK source."""
import shutil
import sys
from pathlib import Path

root = Path(sys.argv[1])
java = root / "liblinphone/wrappers/java/classes/org/linphone/core/tools"
push = java / "PushNotificationUtils.java"
if not push.is_file():
    raise SystemExit("Expected Linphone push source file is missing")
if (java / "firebase").exists():
    shutil.rmtree(java / "firebase")
push.write_text("""package org.linphone.core.tools;

import android.content.Context;

public class PushNotificationUtils {
    public static void init(Context context) {}
    public static boolean isAvailable(Context context) { return false; }
}
""", encoding="utf-8")

for filename in ("build.gradle.cmake", "upload.gradle.cmake"):
    gradle = root / "cmake/Android/gradle" / filename
    original = gradle.read_text(encoding="utf-8")
    gradle.write_text("".join(line.replace("jcenter()", "mavenCentral()")
                              for line in original.splitlines(keepends=True)
                              if "com.google.firebase" not in line
                              and "com.google.android.gms" not in line), encoding="utf-8")
