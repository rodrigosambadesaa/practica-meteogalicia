package gal.rodrigosambade.tempogalicia.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import gal.rodrigosambade.tempogalicia.api.WolframAlphaApiClient;
import gal.rodrigosambade.tempogalicia.model.Municipality;
import gal.rodrigosambade.tempogalicia.parser.WolframAlphaWeatherParser;
import gal.rodrigosambade.tempogalicia.util.NetworkValidationManager;
import net.i2p.android.router.util.ConnectivityAndInternetAccess;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Independent provider: retains the same network-first/diagnostics-on-failure policy. */
public final class WolframAlphaRepository {
    public interface Callback {
        void onSuccess(String html);
        void onError(Exception exception);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final WolframAlphaApiClient api = new WolframAlphaApiClient();
    private final WolframAlphaWeatherParser parser = new WolframAlphaWeatherParser();

    public void getForecast(Context context, Municipality municipality, String appId, Callback callback) {
        executor.execute(() -> {
            try {
                if (!NetworkValidationManager.canStartRemoteRequest(context))
                    throw new IOException("No existe una red utilizable");
                String payload = api.fetch(context, municipality, appId);
                String html = parser.render(payload, municipality.getName());
                mainHandler.post(() -> callback.onSuccess(html));
            } catch (Exception e) {
                if (e instanceof WolframAlphaApiClient.ApiHttpException
                        || !NetworkValidationManager.isTransportFailure(e)) {
                    mainHandler.post(() -> callback.onError(e));
                    return;
                }
                NetworkValidationManager.checkGeneralInternetAsync(context, result -> {
                    boolean reachable = result != null && result.isReachable();
                    boolean captive = ConnectivityAndInternetAccess
                            .snapshotNetworkState(context).isCaptivePortalDetected();
                    callback.onError(new ForecastRepository.NetworkFailureException(e, reachable, captive));
                });
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
