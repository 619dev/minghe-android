package app.fm619.minghe;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import android.util.Base64;
import com.getcapacitor.JSObject;

final class CertificateProbe {
    private CertificateProbe() {}

    static JSObject probe(String host, int port) throws Exception {
        try {
            handshake(SSLContext.getDefault(), host, port, true);
            return new JSObject();
        } catch (SSLHandshakeException untrusted) {
            // Discovery only. No SIP messages or credentials are sent on this socket.
        }
        X509Certificate[] observed = new X509Certificate[1];
        SSLContext discovery = SSLContext.getInstance("TLS");
        discovery.init(null, new TrustManager[]{new X509TrustManager() {
            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            @Override public void checkClientTrusted(X509Certificate[] chain, String type) {}
            @Override public void checkServerTrusted(X509Certificate[] chain, String type)
                    throws java.security.cert.CertificateException {
                if (chain.length == 0) throw new java.security.cert.CertificateException();
                chain[0].checkValidity();
                observed[0] = chain[0];
            }
        }}, null);
        handshake(discovery, host, port, false);
        byte[] der = observed[0].getEncoded();
        String pem = "-----BEGIN CERTIFICATE-----\n" + Base64.encodeToString(der, Base64.DEFAULT)
            + "-----END CERTIFICATE-----";
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(der);
        StringBuilder fingerprint = new StringBuilder();
        for (byte b : hash) {
            if (fingerprint.length() != 0) fingerprint.append(':');
            fingerprint.append(String.format(java.util.Locale.ROOT, "%02X", b & 255));
        }
        return new JSObject().put("certificate", pem).put("fingerprint", fingerprint.toString());
    }

    private static void handshake(SSLContext context, String host, int port, boolean verifyName) throws Exception {
        try (Socket tcp = new Socket()) {
            tcp.connect(new InetSocketAddress(host, port), 6000);
            tcp.setSoTimeout(6000);
            try (SSLSocket tls = (SSLSocket) context.getSocketFactory().createSocket(tcp, host, port, true)) {
                if (verifyName) {
                    var params = tls.getSSLParameters();
                    params.setEndpointIdentificationAlgorithm("HTTPS");
                    tls.setSSLParameters(params);
                }
                tls.startHandshake();
            }
        }
    }
}
