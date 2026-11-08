package app.fm619.minghe;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override public void onCreate(Bundle savedInstanceState) {
        registerPlugin(SipPlugin.class);
        super.onCreate(savedInstanceState);
        if (SipService.instance == null && new AccountStore(this).isEnabled()
            && (android.os.Build.VERSION.SDK_INT < 33
                || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED)) {
            androidx.core.content.ContextCompat.startForegroundService(this,
                new android.content.Intent(this, SipService.class));
        }
    }
}
