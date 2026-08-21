package gal.rodrigosambade.tempogalicia.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import gal.rodrigosambade.tempogalicia.R;
import gal.rodrigosambade.tempogalicia.model.Forecast;
import gal.rodrigosambade.tempogalicia.model.Municipality;
import gal.rodrigosambade.tempogalicia.notification.WeatherNotificationHelper;
import gal.rodrigosambade.tempogalicia.repository.ForecastRepository;
import gal.rodrigosambade.tempogalicia.util.NetworkValidationManager;
import gal.rodrigosambade.tempogalicia.widget.ForecastAppWidgetProvider;

import net.i2p.android.router.util.ConnectivityAndInternetAccess;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String OFFICIAL_METEOGALICIA_PHONE = "881999654";
    private static final String KEY_SELECTED_MUNICIPALITY_INDEX = "selected_municipality_index";
    private static final String KEY_FORECAST_HTML = "forecast_html";

    private Button btnSeleccionaLocalidad;
    private ImageButton ibtnTelefono;
    private ImageButton ibtnDiagnosticoRed;
    private ImageButton ibtnInfo;
    private WebView webvPronostico;
    private TextView tvEstadoConexion;

    private ConnectivityAndInternetAccess.NetworkObserver networkObserver;
    private ConnectivityAndInternetAccess.Request activeProbeRequest;
    private ForecastRepository forecastRepository;

    private int selectedMunicipalityIndex = -1;
    private String currentHtmlContent = null;
    private List<Municipality> municipalities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        municipalities = Municipality.getDefaultMunicipalities();
        forecastRepository = new ForecastRepository();

        // Inicializar canles de notificación
        WeatherNotificationHelper.createNotificationChannels(this);

        // Solicitar permiso POST_NOTIFICATIONS en Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        btnSeleccionaLocalidad = findViewById(R.id.btn_seleccionaLocalidad);
        webvPronostico = findViewById(R.id.webv_pronostico);
        ibtnTelefono = findViewById(R.id.ibtn_telefono);
        ibtnDiagnosticoRed = findViewById(R.id.ibtn_diagnostico_red);
        ibtnInfo = findViewById(R.id.ibtn_info);
        tvEstadoConexion = findViewById(R.id.tv_estadoConexion);

        webvPronostico.getSettings().setJavaScriptEnabled(false);
        webvPronostico.getSettings().setBuiltInZoomControls(true);
        webvPronostico.getSettings().setDisplayZoomControls(false);

        btnSeleccionaLocalidad.setOnClickListener(view -> openMunicipalitySelector());
        ibtnTelefono.setOnClickListener(view -> dialOfficialContactPhone());
        if (ibtnDiagnosticoRed != null) {
            ibtnDiagnosticoRed.setOnClickListener(view -> openNetworkDiagnostics());
        }
        if (ibtnInfo != null) {
            ibtnInfo.setOnClickListener(view -> showAboutDialog());
        }

        tvEstadoConexion.setOnClickListener(view -> {
            updateNetworkStateDisplay(ConnectivityAndInternetAccess.snapshotNetworkState(this));
        });

        if (savedInstanceState != null) {
            selectedMunicipalityIndex = savedInstanceState.getInt(KEY_SELECTED_MUNICIPALITY_INDEX, -1);
            currentHtmlContent = savedInstanceState.getString(KEY_FORECAST_HTML, null);

            if (selectedMunicipalityIndex >= 0 && selectedMunicipalityIndex < municipalities.size()) {
                btnSeleccionaLocalidad.setText(municipalities.get(selectedMunicipalityIndex).getName());
            }

            if (currentHtmlContent != null) {
                webvPronostico.loadDataWithBaseURL(
                        "https://servizos.meteogalicia.gal/",
                        currentHtmlContent,
                        "text/html",
                        "UTF-8",
                        null);
            } else if (selectedMunicipalityIndex >= 0 && selectedMunicipalityIndex < municipalities.size()) {
                loadForecast(municipalities.get(selectedMunicipalityIndex));
            } else {
                webvPronostico.restoreState(savedInstanceState);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_MUNICIPALITY_INDEX, selectedMunicipalityIndex);
        if (currentHtmlContent != null) {
            outState.putString(KEY_FORECAST_HTML, currentHtmlContent);
        }
        webvPronostico.saveState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        networkObserver = ConnectivityAndInternetAccess.observeNetwork(
                this,
                this::updateNetworkStateDisplay);
    }

    @Override
    protected void onStop() {
        if (activeProbeRequest != null) {
            activeProbeRequest.cancel();
            activeProbeRequest = null;
        }
        if (networkObserver != null) {
            networkObserver.close();
            networkObserver = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (activeProbeRequest != null) {
            activeProbeRequest.cancel();
            activeProbeRequest = null;
        }
        if (forecastRepository != null) {
            forecastRepository.shutdown();
        }
        super.onDestroy();
    }

    private void updateNetworkStateDisplay(ConnectivityAndInternetAccess.NetworkState state) {
        if (state == null || !state.isConnected()) {
            tvEstadoConexion.setText(R.string.conexion_sen_conexion);
            tvEstadoConexion.setTextColor(0xFFB3261E);
            return;
        }

        if (state.isCaptivePortalDetected()) {
            tvEstadoConexion.setText(R.string.conexion_portal_cautivo);
            tvEstadoConexion.setTextColor(0xFFB06000);
            return;
        }

        tvEstadoConexion.setText(R.string.conexion_disponible_sen_validar);
        tvEstadoConexion.setTextColor(0xFFB06000);

        if (activeProbeRequest != null) {
            activeProbeRequest.cancel();
        }

        activeProbeRequest = NetworkValidationManager.checkMeteoGaliciaDomainAsync(this, result -> {
            if (!isFinishing() && !isDestroyed()) {
                if (result != null && result.isReachable()) {
                    tvEstadoConexion.setText(R.string.conexion_conectado);
                    tvEstadoConexion.setTextColor(0xFF188038);
                } else {
                    tvEstadoConexion.setText(R.string.conexion_dominios_inalcanzables);
                    tvEstadoConexion.setTextColor(0xFFB06000);
                }
            }
        });
    }

    private void openMunicipalitySelector() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.titulo_seleccionar_municipio);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, 0);

        android.widget.EditText searchBox = new android.widget.EditText(this);
        searchBox.setHint(R.string.titulo_seleccionar_municipio);
        searchBox.setSingleLine(true);
        searchBox.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0);
        layout.addView(searchBox);

        android.widget.ListView listView = new android.widget.ListView(this);
        layout.addView(listView);

        List<Municipality> filteredList = new ArrayList<>(municipalities);
        List<String> displayNames = new ArrayList<>();
        for (Municipality m : filteredList) {
            displayNames.add(m.getName());
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_single_choice,
                displayNames);
        listView.setAdapter(adapter);
        listView.setChoiceMode(android.widget.AbsListView.CHOICE_MODE_SINGLE);

        if (selectedMunicipalityIndex >= 0 && selectedMunicipalityIndex < municipalities.size()) {
            Municipality selected = municipalities.get(selectedMunicipalityIndex);
            int filterPos = filteredList.indexOf(selected);
            if (filterPos >= 0) {
                listView.setItemChecked(filterPos, true);
                listView.setSelection(filterPos);
            }
        }

        builder.setView(layout);
        builder.setNegativeButton(R.string.boton_cancelar, null);

        AlertDialog dialog = builder.create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Municipality chosen = filteredList.get(position);
            selectedMunicipalityIndex = municipalities.indexOf(chosen);
            dialog.dismiss();
            loadForecast(chosen);
        });

        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                String normalizedQuery = stripAccents(query);

                filteredList.clear();
                displayNames.clear();

                for (Municipality m : municipalities) {
                    String normName = stripAccents(m.getName().toLowerCase());
                    if (normName.contains(normalizedQuery)) {
                        filteredList.add(m);
                        displayNames.add(m.getName());
                    }
                }

                adapter.notifyDataSetChanged();

                if (selectedMunicipalityIndex >= 0 && selectedMunicipalityIndex < municipalities.size()) {
                    Municipality selected = municipalities.get(selectedMunicipalityIndex);
                    int filterPos = filteredList.indexOf(selected);
                    if (filterPos >= 0) {
                        listView.setItemChecked(filterPos, true);
                    } else {
                        listView.setItemChecked(-1, false);
                    }
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        dialog.show();
    }

    private static String stripAccents(String s) {
        if (s == null) return "";
        String nfkd = Normalizer.normalize(s, Normalizer.Form.NFD);
        return nfkd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private void loadForecast(Municipality municipality) {
        if (!ConnectivityAndInternetAccess.isConnected(this)) {
            Toast.makeText(this, R.string.error_sen_conexion, Toast.LENGTH_SHORT).show();
            showNetworkFailureDialog(municipality, getString(R.string.error_sen_conexion));
            return;
        }

        btnSeleccionaLocalidad.setEnabled(false);
        btnSeleccionaLocalidad.setText(R.string.comprobando_dominios);

        // Validación asíncrona explícita de dominios de MeteoGalicia antes de realizar a petición
        NetworkValidationManager.checkMeteoGaliciaDomainAsync(this, result -> {
            if (isFinishing() || isDestroyed()) return;

            if (result == null || !result.isReachable()) {
                btnSeleccionaLocalidad.setEnabled(true);
                btnSeleccionaLocalidad.setText(municipality.getName());
                updateNetworkStateDisplay(ConnectivityAndInternetAccess.snapshotNetworkState(MainActivity.this));

                String errorMsg = getString(R.string.error_dominios_meteogalicia,
                        result != null ? result.getAttemptedHosts().toString() : "hosts desconoñecidos");
                showNetworkFailureDialog(municipality, errorMsg);
                return;
            }

            btnSeleccionaLocalidad.setText(getString(R.string.cargando_municipio, municipality.getName()));

            forecastRepository.getForecast(municipality, new ForecastRepository.ForecastCallback() {
                @Override
                public void onSuccess(Forecast forecast, String htmlContent) {
                    currentHtmlContent = htmlContent;
                    btnSeleccionaLocalidad.setEnabled(true);
                    btnSeleccionaLocalidad.setText(municipality.getName());
                    webvPronostico.loadDataWithBaseURL(
                            "https://servizos.meteogalicia.gal/",
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null);

                    // Emitir notificación de previsión e actualizar widgets
                    WeatherNotificationHelper.showWeatherForecastNotification(MainActivity.this, municipality, forecast);
                    ForecastAppWidgetProvider.updateAllWidgets(MainActivity.this);
                }

                @Override
                public void onError(Exception exception) {
                    android.util.Log.e("MainActivity", "Error fetching forecast from MeteoGalicia", exception);
                    btnSeleccionaLocalidad.setEnabled(true);
                    btnSeleccionaLocalidad.setText(municipality.getName());
                    Toast.makeText(MainActivity.this, R.string.error_obtenendo_pronostico, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void showNetworkFailureDialog(Municipality municipality, String reasonMessage) {
        WeatherNotificationHelper.showNetworkAlertNotification(this, reasonMessage);

        new AlertDialog.Builder(this)
                .setTitle(R.string.atencion_titulo)
                .setMessage(reasonMessage)
                .setPositiveButton(R.string.boton_reintentar, (dialog, which) -> loadForecast(municipality))
                .setNeutralButton(R.string.boton_datos_offline, (dialog, which) -> fetchOfflineForecastFallback(municipality))
                .setNegativeButton(R.string.boton_diagnostico, (dialog, which) -> openNetworkDiagnostics())
                .show();
    }

    private void fetchOfflineForecastFallback(Municipality municipality) {
        btnSeleccionaLocalidad.setEnabled(false);
        btnSeleccionaLocalidad.setText(getString(R.string.cargando_municipio, municipality.getName()));

        forecastRepository.getForecast(municipality, new ForecastRepository.ForecastCallback() {
            @Override
            public void onSuccess(Forecast forecast, String htmlContent) {
                currentHtmlContent = htmlContent;
                btnSeleccionaLocalidad.setEnabled(true);
                btnSeleccionaLocalidad.setText(municipality.getName());
                webvPronostico.loadDataWithBaseURL(
                        "https://servizos.meteogalicia.gal/",
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null);

                WeatherNotificationHelper.showWeatherForecastNotification(MainActivity.this, municipality, forecast);
                ForecastAppWidgetProvider.updateAllWidgets(MainActivity.this);
            }

            @Override
            public void onError(Exception exception) {
                btnSeleccionaLocalidad.setEnabled(true);
                btnSeleccionaLocalidad.setText(municipality.getName());
            }
        });
    }

    private void openNetworkDiagnostics() {
        Intent intent = new Intent(this, NetworkDiagnosticsActivity.class);
        startActivity(intent);
    }

    private void dialOfficialContactPhone() {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + OFFICIAL_METEOGALICIA_PHONE));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.error_aplicacion_telefono, Toast.LENGTH_LONG).show();
        }
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.acerca_de_titulo)
                .setMessage(R.string.acerca_de_texto)
                .setPositiveButton(R.string.boton_aceptar, null)
                .show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.atencion_titulo)
                .setMessage(R.string.confirmar_sair_mensaxe)
                .setPositiveButton(R.string.sim, (dialog, which) -> finish())
                .setNegativeButton(R.string.non, null)
                .show();
    }
}
