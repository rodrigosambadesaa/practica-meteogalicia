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

/**
 * Xestor de validaciones de rede e dominios para as actividades de Tempo Galicia,
 * encapsulando as comprobacións asíncronas de ConnectivityAndInternetAccess.
 */
public final class NetworkValidationManager {

    public static final String HOST_METEOGALICIA_API = "https://servizos.meteogalicia.gal/";
    public static final String HOST_METEOGALICIA_WEB = "https://www.meteogalicia.gal/";
    public static final String HOST_XUNTA_WEB = "https://www.xunta.gal/";

    public static final List<String> METEOGALICIA_PRIMARY_HOSTS = Collections.unmodifiableList(Arrays.asList(
            HOST_METEOGALICIA_API,
            HOST_METEOGALICIA_WEB,
            HOST_XUNTA_WEB
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

        // Comprobación de MeteoGalicia API & Web
        return ConnectivityAndInternetAccess.checkInternetAsyncDefault(
                appContext,
                DEFAULT_DNS_RESOLVERS,
                METEOGALICIA_PRIMARY_HOSTS,
                resultApi -> {
                    resultsMap.put("MeteoGalicia", resultApi);

                    // Comprobación de Portal Cautivo Estrito
                    ConnectivityAndInternetAccess strictChecker =
                            ConnectivityAndInternetAccess.strictCaptivePortalBuilder().build();

                    strictChecker.checkInternetAsync(appContext, resultCaptive -> {
                        resultsMap.put("CaptivePortalProbe", resultCaptive);

                        // Comprobación xeral fallback de conectividade
                        ConnectivityAndInternetAccess.checkInternetAsyncDefault(
                                appContext,
                                resultGlobal -> {
                                    resultsMap.put("GlobalInternet", resultGlobal);

                                    DiagnosticReport report = new DiagnosticReport(
                                            snapshot,
                                            wifi,
                                            mobile,
                                            ethernet,
                                            vpn,
                                            airplane,
                                            fast,
                                            resultsMap
                                    );

                                    handler.post(() -> callback.onCompleted(report));
                                }
                        );
                    });
                });
    }

    /**
     * Devolve unha cadea lexible co resumo da interfaz activa.
     */
    public static String getActiveInterfaceName(Context context) {
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
