package app.fm619.minghe;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import com.getcapacitor.JSObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.linphone.core.*;

public final class SipService extends Service {
    private static final String ONLINE = "registration";
    private static final String RING = "incoming-call";
    private static final int ONLINE_ID = 1;
    private static final int CALL_ID = 2;
    private final Handler handler = new Handler(Looper.getMainLooper());
    static SipService instance;
    static JSONObject pendingAccount;
    private boolean restartable;
    private long callStarted;
    private String callPeer;
    private boolean callIncoming, callAnswered;
    private NotificationManager notifications;
    private Core core;
    private Account account;
    private String host, extension, authority;
    private MediaEncryption encryption;
    private int registrationCode, callCode;
    private RegistrationState registration = RegistrationState.None;
    private Call call;
    private String sas = "";
    private boolean secured, capture, callFailed;
    private int generation;
    private String appliedConfig;

    static void validate(JSONObject settings) throws Exception {
        String hostname = settings.getString("host");
        if (!validEndpoint(hostname, settings.getInt("port")) || !extensionValid(settings.getString("extension"))
            || settings.getString("password").isEmpty() || settings.getString("password").length() > 512
            || !settings.getString("mode").matches("sdes|zrtp")
            || !settings.getString("language").matches("system|zh|en|ja|ko|ru|fr|es|de")) {
            throw new IllegalArgumentException();
        }
    }

    static boolean validEndpoint(String hostname, int port) {
        return hostname.length() <= 253 && port >= 1 && port <= 65535
            && hostname.matches("(?i)[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)*")
           ;
    }

    static boolean validCertificate(String pem) {
        if (pem.isEmpty()) return true;
        if (pem.length() > 16384 || !pem.matches("-----BEGIN CERTIFICATE-----[\\r\\n]+[A-Za-z0-9+/=\\r\\n]+-----END CERTIFICATE-----")) return false;
        try {
            var input = new java.io.ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII));
            var certificate = (java.security.cert.X509Certificate)
                java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(input);
            certificate.checkValidity();
            return true;
        } catch (java.security.cert.CertificateException exception) { return false; }
    }

    private static boolean extensionValid(String value) { return value.matches("[1-9][0-9]{0,9}"); }

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        notifications = getSystemService(NotificationManager.class);
    }

    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (intent != null && "decline".equals(intent.getAction())) {
            if (incoming() && intent.getIntExtra("generation", -1) == generation) hangup();
            return restartable ? START_STICKY : START_NOT_STICKY;
        }
        try {
            JSONObject settings = pendingAccount;
            pendingAccount = null;
            AccountStore storage = new AccountStore(this);
            if (settings == null && storage.isEnabled()) settings = storage.read();
            if (settings == null) { stopSelf(); return START_NOT_STICKY; }
            restartable = settings.optBoolean("savePassword", true);
            validate(settings);
            createChannels(settings.getString("language"));
            foreground(false);
            if (!settings.toString().equals(appliedConfig) || core == null) {
                stopCore();
                startCore(settings);
                appliedConfig = settings.toString();
            }
            return restartable ? START_STICKY : START_NOT_STICKY;
        } catch (Exception | LinkageError exception) {
            android.util.Log.w("MingheSip", "SIP startup failed: " + exception.getClass().getSimpleName());
            stopCore();
            registration = RegistrationState.Failed;
            if (appliedConfig == null) {
                try { foreground(false); } catch (Exception ignored) { stopSelf(); }
            }
            notifications.notify(ONLINE_ID, onlineNotification());
            return START_NOT_STICKY;
        }
    }

    private void startCore(JSONObject settings) throws Exception {
        host = settings.getString("host");
        extension = settings.getString("extension");
        authority = host + ":" + settings.getInt("port");
        encryption = "zrtp".equals(settings.getString("mode")) ? MediaEncryption.ZRTP : MediaEncryption.SRTP;
        Factory factory = Factory.instance();
        Config config = factory.createConfigFromString("[rtp]\nrtp_no_xmit_on_audio_mute=1\n");
        core = factory.createCoreWithConfig(config, getApplicationContext());
        core.setUserAgent("Minghe", BuildConfig.VERSION_NAME);
        core.setPushNotificationEnabled(false);
        core.setLimeX3DhEnabled(false);
        core.setAutoIterateBackgroundSchedule(20);
        core.setKeepAliveEnabled(true);
        core.setIpv6Enabled(false);
        core.setVideoCaptureEnabled(false);
        core.setVideoDisplayEnabled(false);
        core.setSessionExpiresEnabled(false);
        core.setAvpfMode(AVPFMode.Disabled);
        core.setMaxCalls(1);
        core.setNativeRingingEnabled(false);
        core.setRing(null);
        core.setZrtpSecretsFile(new java.io.File(getFilesDir(), "zrtp-" + scopeId()).getPath());
        core.setZrtpGoClearEnabled(false);
        core.setNatPolicy(core.createNatPolicy());
        core.verifyServerCertificates(true);
        core.verifyServerCn(true);
        String certificate = settings.optString("certificate");
        if (!certificate.isEmpty()) {
            if (!validCertificate(certificate)) throw new IllegalArgumentException();
            core.setRootCaData(certificate + "\n");
        }
        core.addListener(listener);
        DigestAuthenticationPolicy digest = factory.createDigestAuthenticationPolicy();
        digest.setAllowNoQop(false);
        digest.setAllowMd5(settings.optBoolean("md5"));
        core.setDigestAuthenticationPolicy(digest);
        if (core.start() != 0 || !core.isMediaEncryptionSupported(encryption)
            || core.setMediaEncryption(encryption) != 0) throw new IllegalStateException();
        core.setMediaEncryptionMandatory(true);
        if (encryption == MediaEncryption.SRTP) core.setSrtpCryptoSuites("AES_CM_128_HMAC_SHA1_80");
        core.disableChat(Reason.NotAcceptable);
        Transports transports = factory.createTransports();
        transports.setUdpPort(0);
        transports.setTcpPort(0);
        transports.setTlsPort(-1);
        if (core.setTransports(transports) != 0) throw new IllegalStateException();
        for (PayloadType codec : core.getAudioPayloadTypes()) {
            String type = codec.getMimeType();
            codec.enable("PCMU".equalsIgnoreCase(type) || "PCMA".equalsIgnoreCase(type));
        }
        AccountParams params = core.createAccountParams();
        Address identity = factory.createAddress("sip:" + extension + "@" + host);
        Address server = factory.createAddress("sip:" + authority + ";transport=tls");
        if (identity == null || server == null || params.setIdentityAddress(identity) != 0
            || params.setServerAddress(server) != 0) {
            throw new IllegalStateException();
        }
        Address route = server.clone();
        route.setUriParam("lr", null);
        if (params.setRoutesAddresses(new Address[]{route}) != 0) throw new IllegalStateException();
        params.setTransport(TransportType.Tls);
        params.setPushNotificationAllowed(false);
        params.setRemotePushNotificationAllowed(false);
        params.setExpires(300);
        params.setRegisterEnabled(true);
        core.addAuthInfo(factory.createAuthInfo(extension, null, settings.getString("password"), null, null, host));
        account = core.createAccount(params);
        registration = RegistrationState.Progress;
        if (core.addAccount(account) != 0) throw new IllegalStateException();
        core.setDefaultAccount(account);
    }

    private void finishHistory() {
        if (callStarted == 0) return;
        try {
            var preferences = getSharedPreferences("call-history", MODE_PRIVATE);
            String scope = scopeId();
            JSONArray previous = new JSONArray(preferences.getString(scope, "[]"));
            JSONObject entry = new JSONObject().put("peer", callPeer).put("time", callStarted)
                .put("incoming", callIncoming).put("answered", callAnswered);
            JSONArray next = new JSONArray().put(entry);
            for (int i = 0; i < Math.min(99, previous.length()); i++) next.put(previous.getJSONObject(i));
            preferences.edit().putString(scope, next.toString()).apply();
        } catch (Exception ignored) {
        } finally { callStarted = 0; }
    }

    static JSONArray history(Context context, JSONObject settings) {
        try {
            String scope = hash(settings.getString("host") + ":" + settings.getInt("port")
                + "/" + settings.getString("extension"));
            return new JSONArray(context.getSharedPreferences("call-history", MODE_PRIVATE).getString(scope, "[]"));
        } catch (Exception ignored) { return new JSONArray(); }
    }

    private void stopCore() {
        finishHistory();
        if (core != null) {
            core.removeListener(listener);
            core.terminateAllCalls();
            core.stop();
        }
        core = null;
        account = null;
        call = null;
        capture = false;
        registration = RegistrationState.None;
        registrationCode = 0;
        notifications.cancel(CALL_ID);
    }

    @Override public void onDestroy() {
        stopCore();
        handler.removeCallbacksAndMessages(null);
        instance = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    boolean hasCall() { return call != null; }
    private boolean incoming() { return call != null && call.getState() == Call.State.IncomingReceived; }
    private void requireOnline() {
        if (core == null || (registration != RegistrationState.Ok && registration != RegistrationState.Refreshing)) throw new IllegalStateException();
    }
    private Address target(String peer) {
        if (!extensionValid(peer)) throw new IllegalArgumentException();
        return Factory.instance().createAddress("sip:" + peer + "@" + authority + ";transport=tls");
    }
    private CallParams audioParams(Call current) {
        CallParams params = core.createCallParams(current);
        params.setAccount(account);
        params.setVideoEnabled(false);
        params.setMediaEncryption(encryption);
        return params;
    }

    void dial(String peer) {
        requireOnline();
        if (hasCall()) throw new IllegalStateException();
        callCode = 0;
        Address target = target(peer);
        foreground(true);
        core.setMicEnabled(encryption != MediaEncryption.ZRTP);
        if (core.inviteAddressWithParams(target, audioParams(null)) == null) {
            foreground(false);
            throw new IllegalStateException();
        }
    }

    void answer() {
        if (!incoming()) throw new IllegalStateException();
        foreground(true);
        core.setMicEnabled(encryption != MediaEncryption.ZRTP);
        if (call.acceptWithParams(audioParams(call)) != 0) {
            foreground(false);
            throw new IllegalStateException();
        }
    }

    void hangup() { if (call != null) call.terminate(); }

    void verify() {
        if (call == null || !secured || sas.isEmpty()) throw new IllegalStateException();
        call.setAuthenticationTokenVerified(true);
    }

    JSObject status() {
        String state = "idle";
        if (call != null) {
            switch (call.getState()) {
                case IncomingReceived: state = "incoming"; break;
                case OutgoingRinging: state = "ringing"; break;
                case Connected:
                case StreamsRunning: state = secured ? "active" : "securing"; break;
                default: state = "calling";
            }
        } else if (callFailed) state = "failed";
        return new JSObject().put("registration", registration.name()).put("call", state)
            .put("registrationCode", registrationCode).put("callCode", callCode)
            .put("peer", call == null ? "" : call.getRemoteAddress().getUsername())
            .put("sas", sas).put("verified", call != null && call.getAuthenticationTokenVerified());
    }

    private final CoreListenerStub listener = new CoreListenerStub() {
        @Override public void onAccountRegistrationStateChanged(Core source, Account current,
                RegistrationState state, String description) {
            if (current != account) return;
            registration = state;
            ErrorInfo error = state == RegistrationState.Failed ? current.getErrorInfo() : null;
            registrationCode = error == null ? 0 : error.getProtocolCode();
            if (state == RegistrationState.Failed) {
                android.util.Log.w("MingheSip", "SIP registration failed: " + current.getError() + " (" + registrationCode + ")");
            }
            notifications.notify(ONLINE_ID, onlineNotification());
        }

        @Override public void onCallStateChanged(Core source, Call current, Call.State state, String reason) {
            switch (state) {
                case IncomingReceived:
                case OutgoingInit:
                    call = current;
                    generation++;
                    callStarted = System.currentTimeMillis();
                    callPeer = current.getRemoteAddress().getUsername();
                    callIncoming = state == Call.State.IncomingReceived;
                    callAnswered = false;
                    sas = "";
                    secured = false;
                    callFailed = false;
                    if (state == Call.State.IncomingReceived) ring();
                    break;
                case Connected:
                    callAnswered = true;
                    notifications.cancel(CALL_ID);
                    if (encryption == MediaEncryption.ZRTP) {
                        int expectedGeneration = generation;
                        handler.postDelayed(() -> {
                            if (call != null && generation == expectedGeneration && !secured) {
                                callFailed = true;
                                hangup();
                            }
                        }, 12000);
                    }
                    break;
                case StreamsRunning:
                    if (encryption == MediaEncryption.SRTP) {
                        secured = current.getCurrentParams().getMediaEncryption() == MediaEncryption.SRTP;
                        if (!secured) { callFailed = true; hangup(); }
                    }
                    break;
                case Error:
                case End:
                case Released:
                    if (call != current) break;
                    if (state == Call.State.Error) {
                        ErrorInfo error = current.getErrorInfo();
                        callCode = error == null ? 0 : error.getProtocolCode();
                        android.util.Log.w("MingheSip", "SIP call failed: " + callCode);
                    }
                    finishHistory();
                    callFailed |= state == Call.State.Error;
                    call = null;
                    secured = false;
                    sas = "";
                    notifications.cancel(CALL_ID);
                    if (capture) foreground(false);
                    break;
                default: break;
            }
        }

        @Override public void onCallEncryptionChanged(Core source, Call current, boolean enabled, String token) {
            if (current != call || encryption != MediaEncryption.ZRTP) return;
            boolean lostEncryption = secured && !enabled;
            secured = enabled;
            sas = enabled && token != null ? token : "";
            core.setMicEnabled(enabled);
            if (lostEncryption) { callFailed = true; hangup(); }
        }

    };

    private String scopeId() throws Exception { return hash(authority + "/" + extension); }
    private static String hash(String input) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder text = new StringBuilder(64);
        for (byte b : digest) text.append(Character.forDigit((b & 255) >>> 4, 16)).append(Character.forDigit(b & 15, 16));
        return text.toString();
    }

    private void createChannels(String language) {
        android.content.res.Configuration localeConfig = new android.content.res.Configuration(getResources().getConfiguration());
        if (!"system".equals(language)) localeConfig.setLocale(java.util.Locale.forLanguageTag(language));
        localized = createConfigurationContext(localeConfig);
        NotificationChannel ringing = new NotificationChannel(RING, localized.getString(R.string.incoming),
            NotificationManager.IMPORTANCE_HIGH);
        ringing.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build());
        ringing.enableVibration(true);
        notifications.createNotificationChannel(ringing);
        notifications.createNotificationChannel(new NotificationChannel(ONLINE,
            localized.getString(R.string.registration), NotificationManager.IMPORTANCE_LOW));
        notifications.deleteNotificationChannel("encrypted-message");
    }
    private Context localized;

    private PendingIntent open() {
        return PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification onlineNotification() {
        int label = (registration == RegistrationState.Ok || registration == RegistrationState.Refreshing) ? R.string.online
            : registration == RegistrationState.Failed ? R.string.registration_failed : R.string.registration;
        Context strings = localized == null ? this : localized;
        return new NotificationCompat.Builder(this, ONLINE).setContentTitle("Minghe")
            .setContentText(strings.getString(label)).setSmallIcon(R.drawable.ic_phone)
            .setContentIntent(open()).setOngoing(true).setOnlyAlertOnce(true).build();
    }

    private void foreground(boolean microphone) {
        Notification notification = onlineNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            int type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
            if (microphone) type |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            startForeground(ONLINE_ID, notification, type);
        } else if (Build.VERSION.SDK_INT >= 30) {
            startForeground(ONLINE_ID, notification, microphone ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE : 0);
        } else startForeground(ONLINE_ID, notification);
        capture = microphone;
    }

    private void ring() {
        Intent decline = new Intent(this, SipService.class).setAction("decline").putExtra("generation", generation);
        PendingIntent dismiss = PendingIntent.getService(this, generation, decline, PendingIntent.FLAG_IMMUTABLE);
        String peer = call.getRemoteAddress().getUsername();
        Notification notification = new NotificationCompat.Builder(this, RING)
            .setContentTitle(localized.getString(R.string.incoming)).setContentText(peer)
            .setSmallIcon(R.drawable.ic_phone).setContentIntent(open()).setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addAction(0, localized.getString(R.string.decline), dismiss).build();
        notification.flags |= Notification.FLAG_INSISTENT;
        notifications.notify(CALL_ID, notification);
    }
}
