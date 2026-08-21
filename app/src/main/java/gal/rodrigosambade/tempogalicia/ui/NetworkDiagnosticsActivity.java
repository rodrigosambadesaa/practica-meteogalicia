package gal.rodrigosambade.tempogalicia.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import gal.rodrigosambade.tempogalicia.R;
import gal.rodrigosambade.tempogalicia.util.NetworkValidationManager;

import net.i2p.android.router.util.ConnectivityAndInternetAccess;

import java.util.Map;

public class NetworkDiagnosticsActivity extends AppCompatActivity {

    private TextView tvInterfaceInfo;
    private TextView tvDiagnosticLog;
    private ProgressBar pbDiagnostics;
    private Button btnRunDiagnostics;
    private Button btnCopyReport;
    private ImageButton ibtnBack;

    private ConnectivityAndInternetAccess.NetworkObserver networkObserver;
    private ConnectivityAndInternetAccess.Request activeDiagnosticRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_diagnostics);

        ibtnBack = findViewById(R.id.ibtn_back);
        tvInterfaceInfo = findViewById(R.id.tv_interface_info);
        tvDiagnosticLog = findViewById(R.id.tv_diagnostic_log);
        pbDiagnostics = findViewById(R.id.pb_diagnostics);
        btnRunDiagnostics = findViewById(R.id.btn_executar_diagnostico);
        btnCopyReport = findViewById(R.id.btn_copiar_informe);

        if (ibtnBack != null) {
            ibtnBack.setOnClickListener(v -> finish());
        }

        btnRunDiagnostics.setOnClickListener(v -> runFullDiagnostics());

        if (btnCopyReport != null) {
            btnCopyReport.setOnClickListener(v -> copyReportToClipboard());
        }

        updateSystemInterfaceSnapshot();
    }

    @Override
    protected void onStart() {
        super.onStart();
        networkObserver = ConnectivityAndInternetAccess.observeNetwork(this, state -> {
            if (!isFinishing() && !isDestroyed()) {
                updateSystemInterfaceSnapshot();
            }
        });
    }

    @Override
    protected void onStop() {
        if (activeDiagnosticRequest != null) {
            activeDiagnosticRequest.cancel();
            activeDiagnosticRequest = null;
        }
        if (networkObserver != null) {
            networkObserver.close();
            networkObserver = null;
        }
        super.onStop();
    }

    private void updateSystemInterfaceSnapshot() {
        ConnectivityAndInternetAccess.NetworkState state =
                ConnectivityAndInternetAccess.snapshotNetworkState(this);

        String ifaceName = NetworkValidationManager.getActiveInterfaceName(this);
        boolean isValidated = ConnectivityAndInternetAccess.isInternetValidated(this);
        boolean isCaptive = ConnectivityAndInternetAccess.isCaptivePortalDetected(this);
        boolean isFast = ConnectivityAndInternetAccess.isConnectedFast(this);

        StringBuilder sb = new StringBuilder();
        sb.append("• ").append(getString(R.string.estado_interfaz)).append(": ").append(ifaceName).append("\n");
        sb.append("• Conectado: ").append(state.isConnected() ? getString(R.string.si) : getString(R.string.no)).append("\n");
        sb.append("• ").append(getString(R.string.estado_validado_so)).append(": ").append(isValidated ? getString(R.string.si) : getString(R.string.no)).append("\n");
        sb.append("• ").append(getString(R.string.estado_portal_cautivo)).append(": ").append(isCaptive ? getString(R.string.detectado) : getString(R.string.sen_detectar)).append("\n");
        sb.append("• ").append(getString(R.string.estado_velocidade)).append(": ").append(isFast ? getString(R.string.rapida) : getString(R.string.lenta));

        tvInterfaceInfo.setText(sb.toString());
    }

    private void runFullDiagnostics() {
        btnRunDiagnostics.setEnabled(false);
        pbDiagnostics.setVisibility(View.VISIBLE);
        tvDiagnosticLog.setText(R.string.diagnostico_en_proceso);

        if (activeDiagnosticRequest != null) {
            activeDiagnosticRequest.cancel();
        }

        activeDiagnosticRequest = NetworkValidationManager.checkAllDiagnosticsAsync(this, report -> {
            if (isFinishing() || isDestroyed()) return;

            pbDiagnostics.setVisibility(View.GONE);
            btnRunDiagnostics.setEnabled(true);

            StringBuilder sb = new StringBuilder();
            sb.append("=== INFORME DE DIAGNÓSTICO DE REDE ===\n");
            sb.append("Interfaz: ").append(NetworkValidationManager.getActiveInterfaceName(this)).append("\n");
            sb.append("Validado polo SO: ").append(report.getState().isInternetValidated()).append("\n");
            sb.append("Portal Cautivo: ").append(report.getState().isCaptivePortalDetected()).append("\n\n");
            sb.append("--- RESULTADOS DE PROBAS ASÍNCRONAS ---\n");

            for (Map.Entry<String, ConnectivityAndInternetAccess.InternetResult> entry : report.getHostResults().entrySet()) {
                String targetName = entry.getKey();
                ConnectivityAndInternetAccess.InternetResult res = entry.getValue();

                sb.append("\n Target: ").append(targetName).append("\n");
                sb.append("  - Alcanzable: ").append(res.isReachable() ? "SI [OK]" : "NON [FAIL]").append("\n");
                if (res.getReachedHost() != null) {
                    sb.append("  - Host alcanzado: ").append(res.getReachedHost()).append("\n");
                }
                sb.append("  - Tempo transcorrido: ").append(res.getElapsedMilliseconds()).append(" ms\n");
                sb.append("  - Intentos realizados: ").append(res.getAttemptedHosts()).append("\n");
            }

            tvDiagnosticLog.setText(sb.toString());
        });
    }

    private void copyReportToClipboard() {
        CharSequence text = tvDiagnosticLog.getText();
        if (text != null && text.length() > 0) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("NetworkDiagnosticReport", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, R.string.informe_copiado, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
