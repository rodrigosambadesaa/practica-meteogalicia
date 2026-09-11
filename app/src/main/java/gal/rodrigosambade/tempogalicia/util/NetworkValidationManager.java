package gal.rodrigosambade.tempogalicia.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import net.i2p.android.router.util.ConnectivityAndInternetAccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import gal.rodrigosambade.tempogalicia.api.MeteoGaliciaApiClient;
import gal.rodrigosambade.tempogalicia.model.Municipality;

/**
 * Xestor de validaciones de rede e dominios para as actividades de Tempo Galicia,
 * encapsulando as comprobacións asíncronas de ConnectivityAndInternetAccess.
 */
public final class NetworkValidationManager {

    public static final String HOST_METEOGALICIA_API = "https://servizos.meteogalicia.gal/";

    public static final List<String> METEOGALICIA_PRIMARY_HOSTS = Collections.unmodifiableList(Arrays.asList(
            HOST_METEOGALICIA_API
    ));

    public static final List<String> DEFAULT_DNS_RESOLVERS = Collections.unmodifiableList(Arrays.asList(
            "1.1.1.1",
            "8.8.8.8",
            "9.9.9.9",
            "208.67.222.222"
    ));

    public interface DiagnosticsCallback {
        void onCompleted(DiagnosticReport report);
    }

    public static final class DiagnosticReport {
        private final ConnectivityAndInternetAccess.NetworkState state;
        private final boolean isWifi;
        private final boolean isMobile;
        private final boolean isEthernet;
        private final boolean isVpn;
        private final boolean isAirplaneMode;
        private final boolean isFastConnection;
        private final Map<String, ConnectivityAndInternetAccess.InternetResult> hostResults;

        public DiagnosticReport(
                ConnectivityAndInternetAccess.NetworkState state,
                boolean isWifi,
                boolean isMobile,
                boolean isEthernet,
                boolean isVpn,
                boolean isAirplaneMode,
                boolean isFastConnection,
                Map<String, ConnectivityAndInternetAccess.InternetResult> hostResults) {
            this.state = state;
            this.isWifi = isWifi;
            this.isMobile = isMobile;
            this.isEthernet = isEthernet;
            this.isVpn = isVpn;
            this.isAirplaneMode = isAirplaneMode;
            this.isFastConnection = isFastConnection;
            this.hostResults = Collections.unmodifiableMap(new LinkedHashMap<>(hostResults));
        }

        public ConnectivityAndInternetAccess.NetworkState getState() { return state; }
        public boolean isWifi() { return isWifi; }
        public boolean isMobile() { return isMobile; }
        public boolean isEthernet() { return isEthernet; }
        public boolean isVpn() { return isVpn; }
        public boolean isAirplaneMode() { return isAirplaneMode; }
        public boolean isFastConnection() { return isFastConnection; }
        public Map<String, ConnectivityAndInternetAccess.InternetResult> getHostResults() { return hostResults; }
    }

    private NetworkValidationManager() {
        // Clase de utilidade
    }

    /**
     * A VPN local pode manter isConnected() a true sen deixar ningún transporte
     * físico utilizable. Esta é a política común para iniciar novas peticións.
     */
    public static boolean canStartRemoteRequest(Context context) {
        return context != null
                && remoteRequestAllowed(
                ConnectivityAndInternetAccess.isConnected(context),
                ConnectivityAndInternetAccess.hasPhysicalNetwork(context));
    }

    static boolean remoteRequestAllowed(boolean connected, boolean physicalNetwork) {
        return connected && physicalNetwork;
    }

    /**
     * Comproba asíncronamente a conectividade cos dominios de MeteoGalicia.
     */
    public static ConnectivityAndInternetAccess.Request checkMeteoGaliciaDomainAsync(
            Context context,
            ConnectivityAndInternetAccess.InternetCallback callback) {
        return ConnectivityAndInternetAccess.checkInternetAsyncDefault(
                context,
                METEOGALICIA_PRIMARY_HOSTS,
                callback);
    }

    /**
     * Diagnóstico global reservado para el fallback de una operación real que falló
     * por transporte. No se ejecuta como preflight normal de la API.
     */
    public static ConnectivityAndInternetAccess.Request checkGeneralInternetAsync(
            Context context,
            ConnectivityAndInternetAccess.InternetCallback callback) {
        return ConnectivityAndInternetAccess.checkInternetAsyncDefault(context, callback);
    }

    public static boolean isTransportFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof UnknownHostException
                    || current instanceof SocketTimeoutException
                    || current instanceof SocketException
                    || current instanceof ConnectException
                    || current instanceof java.net.NoRouteToHostException
                    || current instanceof IOException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Valida primero el recurso de predicción que la pantalla va a solicitar.
     * Así una red conectada pero sin acceso a MeteoGalicia no dispara la descarga.
     */
    public static ConnectivityAndInternetAccess.Request checkForecastResourceAsync(
            Context context,
            Municipality municipality,
            ConnectivityAndInternetAccess.InternetCallback callback) {
        return ConnectivityAndInternetAccess.checkInternetAsyncDefault(
                context,
                Collections.singletonList(MeteoGaliciaApiClient.buildForecastUrl(municipality)),
                callback);
    }

    /**
     * Comproba asíncronamente unha lista personalizada de dominios/hosts.
     */
    public static ConnectivityAndInternetAccess.Request checkCustomDomainsAsync(
            Context context,
            List<String> hosts,
            ConnectivityAndInternetAccess.InternetCallback callback) {
        return ConnectivityAndInternetAccess.checkInternetAsyncDefault(
                context,
                hosts,
                callback);
    }

    /**
     * Comproba asíncronamente a existencia de portals cautivos usando o Builder estrito.
     */
    public static ConnectivityAndInternetAccess.Request checkStrictCaptivePortalAsync(
            Context context,
            ConnectivityAndInternetAccess.InternetCallback callback) {
        ConnectivityAndInternetAccess strictClient =
                ConnectivityAndInternetAccess.strictCaptivePortalBuilder().build();
        return strictClient.checkInternetAsync(context, callback);
    }

    /**
     * Executa un diagnóstico profundo asíncrono e completo da rede e dominios.
     */
    public static ConnectivityAndInternetAccess.Request checkAllDiagnosticsAsync(
            Context context,
            DiagnosticsCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback == null");
        }

        final Context appContext = context.getApplicationContext() != null
                ? context.getApplicationContext()
                : context;

        final ConnectivityAndInternetAccess.NetworkState snapshot =
                ConnectivityAndInternetAccess.snapshotNetworkState(appContext);
        final boolean wifi = ConnectivityAndInternetAccess.isConnectedWifi(appContext);
        final boolean mobile = ConnectivityAndInternetAccess.isConnectedMobile(appContext);
        final boolean ethernet = ConnectivityAndInternetAccess.isConnectedEthernet(appContext);
        final boolean vpn = ConnectivityAndInternetAccess.vpnActive(appContext);
        final boolean airplane = ConnectivityAndInternetAccess.isAirplaneModeOn(appContext);
        final boolean fast = ConnectivityAndInternetAccess.isConnectedFast(appContext);

        final Map<String, ConnectivityAndInternetAccess.InternetResult> resultsMap = new LinkedHashMap<>();
        final Handler handler = new Handler(Looper.getMainLooper());

        // La operación manual de diagnóstico comienza por el backend real.
        return ConnectivityAndInternetAccess.checkInternetAsyncDefault(
                appContext,
                METEOGALICIA_PRIMARY_HOSTS,
                resultApi -> {
                    resultsMap.put("MeteoGalicia", resultApi);

                    if (resultApi == null || !resultApi.isReachable()) {
                        runFallbackDiagnostics(appContext, resultsMap, snapshot, wifi, mobile,
                                ethernet, vpn, airplane, fast, handler, callback);
                        return;
                    }

                    publishReport(handler, callback, snapshot, wifi, mobile, ethernet,
                            vpn, airplane, fast, resultsMap);
                });
    }

    private static void runFallbackDiagnostics(
            Context context,
            Map<String, ConnectivityAndInternetAccess.InternetResult> resultsMap,
            ConnectivityAndInternetAccess.NetworkState snapshot,
            boolean wifi,
            boolean mobile,
            boolean ethernet,
            boolean vpn,
            boolean airplane,
            boolean fast,
            Handler handler,
            DiagnosticsCallback callback) {
        ConnectivityAndInternetAccess strictChecker =
                ConnectivityAndInternetAccess.strictCaptivePortalBuilder().build();
        strictChecker.checkInternetAsync(context, resultCaptive -> {
            resultsMap.put("CaptivePortalProbe", resultCaptive);
            checkGeneralInternetAsync(context, resultGlobal -> {
                resultsMap.put("GlobalInternet", resultGlobal);
                publishReport(handler, callback, snapshot, wifi, mobile, ethernet,
                        vpn, airplane, fast, resultsMap);
            });
        });
    }

    private static void publishReport(
            Handler handler,
            DiagnosticsCallback callback,
            ConnectivityAndInternetAccess.NetworkState snapshot,
            boolean wifi,
            boolean mobile,
            boolean ethernet,
            boolean vpn,
            boolean airplane,
            boolean fast,
            Map<String, ConnectivityAndInternetAccess.InternetResult> resultsMap) {
        DiagnosticReport report = new DiagnosticReport(snapshot, wifi, mobile, ethernet,
                vpn, airplane, fast, resultsMap);
        handler.post(() -> callback.onCompleted(report));
    }

    /**
     * Devolve unha cadea lexible co resumo da interfaz activa.
     */
    public static String getActiveInterfaceName(Context context) {
        if (!ConnectivityAndInternetAccess.hasPhysicalNetwork(context)) {
            return "Sen transporte físico";
        }
        if (ConnectivityAndInternetAccess.vpnActive(context)) {
            return "VPN";
        }
        if (ConnectivityAndInternetAccess.isConnectedWifi(context)) {
            return "Wi-Fi";
        }
        if (ConnectivityAndInternetAccess.isConnectedMobile(context)) {
            return "Datos Móbiles";
        }
        if (ConnectivityAndInternetAccess.isConnectedEthernet(context)) {
            return "Ethernet";
        }
        if (ConnectivityAndInternetAccess.isAirplaneModeOn(context)) {
            return "Modo Avión";
        }
        return "Descoñecida";
    }
}
