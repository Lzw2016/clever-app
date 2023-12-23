package org.clever.core.http;

import lombok.SneakyThrows;
import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 忽略 X509 数字证书
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/12/23 14:16 <br/>
 */
public class IgnoreX509TrustManager implements X509TrustManager {
    public static final IgnoreX509TrustManager INSTANCE = new IgnoreX509TrustManager();

    @Override
    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
    }

    @Override
    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
    }

    @Override
    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return new java.security.cert.X509Certificate[]{};
    }

    /**
     * 设置忽略X509数字证书 (Https证书)
     */
    @SneakyThrows
    public static void ignoreX509(OkHttpClient.Builder builder) {
        final TrustManager[] trustAllCerts = new TrustManager[]{INSTANCE};
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
    }
}
