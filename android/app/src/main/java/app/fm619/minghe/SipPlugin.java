package app.fm619.minghe;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import org.json.JSONObject;

@CapacitorPlugin(name = "Sip", permissions = {
    @Permission(alias = "microphone", strings = { Manifest.permission.RECORD_AUDIO }),
    @Permission(alias = "notifications", strings = { Manifest.permission.POST_NOTIFICATIONS })
})
public class SipPlugin extends Plugin {
    private JSONObject accountSummary;
    private boolean accountLoaded;

    private void rememberAccount(JSONObject account) throws org.json.JSONException {
        accountSummary = account == null ? null : new JSONObject(account.toString());
        if (accountSummary != null) {
            accountSummary.put("hasSavedPassword", accountSummary.optBoolean("savePassword", true)
                && !accountSummary.optString("password").isEmpty());
            accountSummary.remove("password");
        }
        accountLoaded = true;
    }
    @PluginMethod public void setAppearance(PluginCall call) {
        boolean dark = call.getBoolean("dark", false);
        getActivity().runOnUiThread(() -> {
            android.view.Window window = getActivity().getWindow();
            android.view.View decor = window.getDecorView();
            int color = dark ? 0xff102821 : 0xfff0f5ef;
            decor.setBackgroundColor(color);
            if (Build.VERSION.SDK_INT < 35) {
                window.setStatusBarColor(color);
                window.setNavigationBarColor(color);
            }
            var insets = androidx.core.view.WindowCompat.getInsetsController(window, decor);
            insets.setAppearanceLightStatusBars(!dark);
            insets.setAppearanceLightNavigationBars(!dark);
            call.resolve();
        });
    }

    @PluginMethod public void discoverCertificate(PluginCall call) {
        String host = call.getString("host", "");
        int port = call.getInt("port", 0);
        if (!SipService.validEndpoint(host, port)) { call.reject("connectError"); return; }
        getBridge().execute(() -> {
            try { call.resolve(CertificateProbe.probe(host, port)); }
            catch (Exception exception) { call.reject("connectError"); }
        });
    }

    @PluginMethod public void connect(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33 && getPermissionState("notifications") != PermissionState.GRANTED) {
            requestPermissionForAlias("notifications", call, "afterNotification");
        } else afterNotification(call);
    }

    @PermissionCallback private void afterNotification(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33 && getPermissionState("notifications") != PermissionState.GRANTED) {
            call.reject("notifications"); return;
        }
        getActivity().runOnUiThread(() -> {
            try {
                JSONObject account = call.getObject("account");
                AccountStore storage = new AccountStore(getContext());
                if (account != null && account.optString("password").isEmpty()) {
                    JSONObject saved = storage.read();
                    if (saved != null && saved.getString("host").equals(account.optString("host"))
                        && saved.getString("extension").equals(account.optString("extension"))
                        && saved.getInt("port") == account.optInt("port")) {
                        account.put("password", saved.optString("password"));
                    }
                }
                SipService.validate(account);
                if (!SipService.validCertificate(account.optString("certificate"))) {
                    call.reject("invalidCertificate"); return;
                }
                if (SipService.instance != null && SipService.instance.hasCall()) {
                    call.reject("busy"); return;
                }
                storage.save(account);
                rememberAccount(account);
                SipService.pendingAccount = account;
                try {
                    ContextCompat.startForegroundService(getContext(), new Intent(getContext(), SipService.class));
                } catch (Exception exception) {
                    SipService.pendingAccount = null;
                    throw exception;
                }
                call.resolve();
            } catch (Exception exception) { call.reject("connectError"); }
        });
    }

    @PluginMethod public void disconnect(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            if (!new AccountStore(getContext()).disable()) { call.reject("connectError"); return; }
            SipService.pendingAccount = null;
            getContext().stopService(new Intent(getContext(), SipService.class));
            call.resolve();
        });
    }

    @PluginMethod public void status(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            JSObject status = SipService.instance == null ? new JSObject() : SipService.instance.status();
            if (!accountLoaded) {
                try { rememberAccount(new AccountStore(getContext()).read()); }
                catch (Exception ignored) { accountLoaded = true; }
            }
            if (accountSummary != null) {
                status.put("account", accountSummary);
                status.put("history", SipService.history(getContext(), accountSummary));
            }
            call.resolve(status);
        });
    }

    @PluginMethod public void batterySettings(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            try {
                getActivity().startActivity(new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                call.resolve();
            } catch (Exception exception) { call.reject("connectError"); }
        });
    }

    @PluginMethod public void dial(PluginCall call) { withMicrophone(call); }
    @PluginMethod public void answer(PluginCall call) { withMicrophone(call); }
    private void withMicrophone(PluginCall call) {
        if (getPermissionState("microphone") != PermissionState.GRANTED) {
            requestPermissionForAlias("microphone", call, "callWithPermission");
        } else callWithPermission(call);
    }
    @PermissionCallback private void callWithPermission(PluginCall call) {
        if (getPermissionState("microphone") != PermissionState.GRANTED) {
            call.reject("microphone"); return;
        }
        run(call, service -> {
            if ("answer".equals(call.getMethodName())) service.answer();
            else service.dial(call.getString("peer", ""));
        });
    }

    @PluginMethod public void hangup(PluginCall call) { run(call, SipService::hangup); }
    @PluginMethod public void verify(PluginCall call) { run(call, SipService::verify); }
    private interface Command { void invoke(SipService service); }
    private void run(PluginCall call, Command command) {
        getActivity().runOnUiThread(() -> {
            try {
                if (SipService.instance == null) { call.reject("offline"); return; }
                command.invoke(SipService.instance);
                call.resolve();
            } catch (Exception exception) { call.reject("callError"); }
        });
    }
}
