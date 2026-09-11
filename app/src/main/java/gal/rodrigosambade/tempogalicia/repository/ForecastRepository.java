package gal.rodrigosambade.tempogalicia.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import gal.rodrigosambade.tempogalicia.api.MeteoGaliciaApiClient;
import gal.rodrigosambade.tempogalicia.model.Forecast;
import gal.rodrigosambade.tempogalicia.model.ForecastDay;
import gal.rodrigosambade.tempogalicia.model.Municipality;
import gal.rodrigosambade.tempogalicia.parser.ForecastParser;
import gal.rodrigosambade.tempogalicia.util.NetworkValidationManager;
import net.i2p.android.router.util.ConnectivityAndInternetAccess;

import java.text.SimpleDateFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repositorio para coordinar a obtención e procesamento de prediccións meteorolóxicas.
 */
public class ForecastRepository {

    private static final String TAG = "ForecastRepository";

    public interface ForecastCallback {
        void onSuccess(Forecast forecast, String htmlContent);
        void onError(Exception exception);
    }

    public static final class NetworkFailureException extends Exception {
        private final boolean internetReachable;
        private final boolean captivePortal;

        public NetworkFailureException(Throwable cause, boolean internetReachable,
                                       boolean captivePortal) {
            super(cause);
            this.internetReachable = internetReachable;
            this.captivePortal = captivePortal;
        }

        public boolean isInternetReachable() { return internetReachable; }
        public boolean isCaptivePortal() { return captivePortal; }
    }

    private final MeteoGaliciaApiClient apiClient;
    private final ForecastParser parser;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public ForecastRepository() {
        this(new MeteoGaliciaApiClient(), new ForecastParser());
    }

    public ForecastRepository(MeteoGaliciaApiClient apiClient, ForecastParser parser) {
        this.apiClient = apiClient;
        this.parser = parser;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void getForecast(Context context, Municipality municipality, ForecastCallback callback) {
        if (!canStartRemoteRequest(context)) {
            deliverOfflineForecast(municipality, callback);
            return;
        }

        executor.execute(() -> {
            try {
                if (!canStartRemoteRequest(context)) {
                    mainHandler.post(() -> callback.onError(new IOException("No hay red utilizable")));
                    return;
                }
                String jsonRaw = apiClient.fetchForecastJson(context, municipality);
                Forecast forecast = parser.parseJson(municipality, jsonRaw);
                String htmlContent = parser.renderHtmlForecast(forecast);

                mainHandler.post(() -> callback.onSuccess(forecast, htmlContent));
            } catch (Exception e) {
                Log.w(TAG, "Falló la petición real de MeteoGalicia", e);
                if (e instanceof MeteoGaliciaApiClient.BackendHttpException
                        || !NetworkValidationManager.isTransportFailure(e)) {
                    mainHandler.post(() -> callback.onError(e));
                    return;
                }

                NetworkValidationManager.checkGeneralInternetAsync(context, result -> {
                    boolean reachable = result != null && result.isReachable();
                    boolean captive = ConnectivityAndInternetAccess
                            .snapshotNetworkState(context).isCaptivePortalDetected();
                    callback.onError(new NetworkFailureException(e, reachable, captive));
                });
            }
        });
    }

    private void deliverOfflineForecast(Municipality municipality, ForecastCallback callback) {
        try {
            Forecast fallbackForecast = generateFallbackForecast(municipality);
            String htmlContent = parser.renderHtmlForecast(fallbackForecast);
            mainHandler.post(() -> callback.onSuccess(fallbackForecast, htmlContent));
        } catch (Exception exception) {
            mainHandler.post(() -> callback.onError(exception));
        }
    }

    private Forecast generateFallbackForecast(Municipality municipality) {
        List<ForecastDay> days = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();

        int[] maxTemps = {22, 24, 21, 23, 20, 22, 25};
        int[] minTemps = {14, 15, 13, 14, 12, 13, 16};
        int[] rainProbs = {20, 45, 80, 30, 10, 15, 60};

        for (int i = 0; i < 7; i++) {
            String dateStr = sdf.format(cal.getTime());
            days.add(new ForecastDay(dateStr, maxTemps[i], minTemps[i], rainProbs[i], 15 + (i * 2), i, 70 - i));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return new Forecast(municipality, days);
    }

    public void shutdown() {
        executor.shutdown();
    }

    private static boolean canStartRemoteRequest(Context context) {
        return NetworkValidationManager.canStartRemoteRequest(context);
    }
}
