package gal.rodrigosambade.tempogalicia.api;

import android.content.Context;
import android.util.Log;

import gal.rodrigosambade.tempogalicia.model.Municipality;
import net.i2p.android.router.util.ConnectivityAndInternetAccess;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Cliente HTTP para a consulta directa do servizo público de MeteoGalicia / Xunta de Galicia.
 */
public class MeteoGaliciaApiClient {

    private static final String TAG = "MeteoGaliciaApiClient";
    private static final String BASE_URL_HTTPS = "https://servizos.meteogalicia.gal/mgrss/predicion/jsonPredMedioPrazo.action?idConc=";
    private static final String BASE_URL_HTTP = "http://servizos.meteogalicia.gal/mgrss/predicion/jsonPredMedioPrazo.action?idConc=";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 14; SM-S938B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 TempoGalicia/1.0";

    public static String buildForecastUrl(Municipality municipality) {
        if (municipality == null) {
            throw new IllegalArgumentException("Municipality cannot be null");
        }
        return BASE_URL_HTTPS + municipality.getCode();
    }

    public String fetchForecastJson(Context context, Municipality municipality) throws IOException {
        if (!canStartRemoteRequest(context)) {
            throw new IOException("No hay una red física utilizable");
        }
        try {
            return fetchWithRedirects(context, BASE_URL_HTTPS + municipality.getCode());
        } catch (IOException e) {
            Log.w(TAG, "HTTPS request failed, retrying with HTTP fallback", e);
            if (!canStartRemoteRequest(context)) {
                throw new IOException("La conexión física se perdió durante la consulta", e);
            }
            return fetchWithRedirects(context, BASE_URL_HTTP + municipality.getCode());
        }
    }

    private String fetchWithRedirects(Context context, String initialUrl) throws IOException {
        String currentUrl = initialUrl;
        String cookie = null;

        for (int redirectCount = 0; redirectCount < 5; redirectCount++) {
            if (!canStartRemoteRequest(context)) {
                throw new IOException("No hay una red física utilizable");
            }
            URL url = new URL(currentUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setUseCaches(false);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("Accept", "application/json, text/plain, */*");
                connection.setRequestProperty("User-Agent", USER_AGENT);
                if (cookie != null) {
                    connection.setRequestProperty("Cookie", cookie);
                }

                int responseCode = connection.getResponseCode();

                // Guardar cookie si la envía el servidor
                String setCookie = connection.getHeaderField("Set-Cookie");
                if (setCookie != null) {
                    cookie = setCookie.split(";")[0];
                }

                // Manejo de redirecciones 301, 302, 303, 307, 308
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || responseCode == HttpURLConnection.HTTP_SEE_OTHER
                        || responseCode == 307
                        || responseCode == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location != null && !location.isEmpty()) {
                        Log.d(TAG, "Redirecting (" + responseCode + ") to: " + location);
                        currentUrl = location;
                        continue;
                    }
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "MeteoGalicia API HTTP Error: " + responseCode + " for URL: " + currentUrl);
                    throw new IOException("HTTP error from MeteoGalicia: " + responseCode);
                }

                try (InputStream inputStream = connection.getInputStream()) {
                    return readInputStream(inputStream);
                }
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("Too many redirects fetching MeteoGalicia forecast");
    }

    private static boolean canStartRemoteRequest(Context context) {
        return context != null
                && ConnectivityAndInternetAccess.isConnected(context)
                && ConnectivityAndInternetAccess.hasPhysicalNetwork(context);
    }

    private String readInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toString(StandardCharsets.UTF_8.name());
    }
}
