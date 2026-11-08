package app.fm619.minghe;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class AccountStore {
    private static final String KEY = "minghe-sip-account";
    private final Context context;

    AccountStore(Context context) { this.context = context; }

    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(KEY)) return (SecretKey) store.getKey(KEY, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY,
            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }

    boolean isEnabled() {
        return context.getSharedPreferences(KEY, Context.MODE_PRIVATE).getBoolean("enabled", false);
    }

    boolean disable() {
        return context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit().putBoolean("enabled", false).commit();
    }

    void save(JSONObject account) throws Exception {
        JSONObject stored = new JSONObject(account.toString());
        boolean remember = stored.optBoolean("savePassword", true);
        if (!remember) stored.remove("password");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = cipher.doFinal(stored.toString().getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        byte[] data = Arrays.copyOf(iv, iv.length + encrypted.length);
        System.arraycopy(encrypted, 0, data, iv.length, encrypted.length);
        if (!context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit()
            .putString("account", Base64.encodeToString(data, Base64.NO_WRAP)).putBoolean("enabled", remember).commit()) {
            throw new IllegalStateException("Account storage failed");
        }
    }

    JSONObject read() throws Exception {
        String value = context.getSharedPreferences(KEY, Context.MODE_PRIVATE).getString("account", null);
        if (value == null) return null;
        byte[] data = Base64.decode(value, Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, data, 0, 12));
        return new JSONObject(new String(cipher.doFinal(data, 12, data.length - 12), StandardCharsets.UTF_8));
    }
}
