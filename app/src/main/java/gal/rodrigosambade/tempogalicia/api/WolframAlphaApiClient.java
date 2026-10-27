package gal.rodrigosambade.tempogalicia.api;

import android.content.Context;
import gal.rodrigosambade.tempogalicia.model.Municipality;
import gal.rodrigosambade.tempogalicia.util.NetworkValidationManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Official Wolfram|Alpha Full Results API; never embeds an AppID in source control. */
public class WolframAlphaApiClient {
    private static final String ENDPOINT = "https://api.wolframalpha.com/v2/query";
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;

    public static final class ApiHttpException extends IOException {
        private final int status;
        public ApiHttpException(int status) {
            super("Wolfram|Alpha ha respondido con HTTP " + status);
            this.status = status;
        }
        public int getStatus() { return status; }
    }

    public static String buildQuery(Municipality municipality) {
        if (municipality == null) throw new IllegalArgumentException("Municipio obligatorio");
        return "weather " + municipality.getName();
    }

    public static String buildRequestUrl(Municipality municipality, String appId) {
        if (appId == null || appId.trim().isEmpty())
            throw new IllegalArgumentException("El AppID de Wolfram|Alpha es obligatorio");
        try {
            return ENDPOINT + "?appid=" + URLEncoder.encode(appId.trim(), "UTF-8")
                    + "&input=" + URLEncoder.encode(buildQuery(municipality), "UTF-8")
                    + "&location=" + URLEncoder.encode("Galicia, Spain", "UTF-8")
                    + "&output=json&format=plaintext&units=metric&timeout=15";
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public String fetch(Context context, Municipality municipality, String appId) throws IOException {
        if (!NetworkValidationManager.canStartRemoteRequest(context))
            throw new IOException("No existe una red utilizable");
        HttpURLConnection connection = (HttpURLConnection) new URL(buildRequestUrl(municipality, appId)).openConnection();
        try {
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "TempoGalicia/1.1 (Android)");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new ApiHttpException(status);
            try (InputStream in = connection.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    if (out.size() + n > MAX_RESPONSE_BYTES)
                        throw new IOException("Respuesta demasiado grande de Wolfram|Alpha");
                    out.write(buffer, 0, n);
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }
}
