package gal.rodrigosambade.tempogalicia.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.EditText;
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
import gal.rodrigosambade.tempogalicia.api.MeteoGaliciaApiClient;
import gal.rodrigosambade.tempogalicia.model.Forecast;
import gal.rodrigosambade.tempogalicia.model.Municipality;
import gal.rodrigosambade.tempogalicia.notification.WeatherNotificationHelper;
import gal.rodrigosambade.tempogalicia.repository.ForecastRepository;
import gal.rodrigosambade.tempogalicia.repository.WolframAlphaRepository;
import gal.rodrigosambade.tempogalicia.api.WolframAlphaApiClient;
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
    private static final String KEY_SELECTED_SOURCE = "selected_source";
    private static final String PREFS = "weather_settings";
    private static final String APP_ID_KEY = "wolfram_app_id";
    private static final int METEOGALICIA = 0;
    private static final int WOLFRAM = 1;

    private Button btnSeleccionaLocalidad;
    private Button btnSeleccionaFuente;
    private ImageButton ibtnTelefono;
    private ImageButton ibtnDiagnosticoRed;
    private ImageButton ibtnInfo;
    private WebView webvPronostico;
    private TextView tvEstadoConexion;

    private ConnectivityAndInternetAccess.NetworkObserver networkObserver;
    private ForecastRepository forecastRepository;
    private WolframAlphaRepository wolframRepository;
    private int selectedSource = METEOGALICIA;
    private int requestGeneration = 0;

    private int selectedMunicipalityIndex = -1;
    private String currentHtmlContent = null;
    private List<Municipality> municipalities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        municipalities = Municipality.getDefaultMunicipalities();
        forecastRepository = new ForecastRepository();
        wolframRepository = new WolframAlphaRepository();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        selectedSource = prefs.getInt(KEY_SELECTED_SOURCE, METEOGALICIA);
        selectedMunicipalityIndex = prefs.getInt(KEY_SELECTED_MUNICIPALITY_INDEX, -1);

        // Inicializar canles de notificación
        WeatherNotificationHelper.createNotificationChannels(this);

        // Solicitar permiso POST_NOTIFICATIONS en Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        btnSeleccionaLocalidad = findViewById(R.id.btn_seleccionaLocalidad);
        btnSeleccionaFuente = findViewById(R.id.btn_seleccionaFuente);
        webvPronostico = findViewById(R.id.webv_pronostico);
        ibtnTelefono = findViewById(R.id.ibtn_telefono);
        ibtnDiagnosticoRed = findViewById(R.id.ibtn_diagnostico_red);
        ibtnInfo = findViewById(R.id.ibtn_info);
        tvEstadoConexion = findViewById(R.id.tv_estadoConexion);

        webvPronostico.getSettings().setJavaScriptEnabled(false);
        webvPronostico.getSettings().setBuiltInZoomControls(true);
        webvPronostico.getSettings().setDisplayZoomControls(false);

        btnSeleccionaLocalidad.setOnClickListener(view -> openMunicipalitySelector());
        btnSeleccionaFuente.setOnClickListener(view -> openSourceSelector());
        updateSourceLabel();
        if (selectedMunicipalityIndex >= 0 && selectedMunicipalityIndex < municipalities.size())
            btnSeleccionaLocalidad.setText(municipalities.get(selectedMunicipalityIndex).getName());
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
            selectedSource = savedInstanceState.getInt(KEY_SELECTED_SOURCE, METEOGALICIA);
            updateSourceLabel();
            currentHtmlContent = savedInstanceState.getString(KEY_FORECAST_HTML, null);

            if (selectedMunicipalityIndex >= 0 && selectedMunicipalityIndex < municipalities.size()) {
                btnSeleccionaLocalidad.setText(municipalities.get(selectedMunicipalityIndex).getName());
            }

            if (currentHtmlContent != null) {
                webvPronostico.loadDataWithBaseURL(
                        selectedSource == METEOGALICIA
                                ? "https://servizos.meteogalicia.gal/" : "https://api.wolframalpha.com/",
                        currentHtmlContent,
                        "text/html",
                        "UTF-8",
                        null);
            } else if (selectedMunicipalityIndex >= 0 && selectedMunicipalityIndex < municipalities.size()) {
                loadForecast(municipalities.get(selectedMunicipalityIndex));
            } else {
                webvPronostico.restoreState(savedInstanceState);
            }
        } else if (selectedMunicipalityIndex >= 0 && selectedMunicipalityIndex < municipalities.size()) {
            loadForecast(municipalities.get(selectedMunicipalityIndex));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_MUNICIPALITY_INDEX, selectedMunicipalityIndex);
        outState.putInt(KEY_SELECTED_SOURCE, selectedSource);
        // Wolfram|Alpha API terms prohibit caching its results; only MeteoGalicia HTML is saved.
        if (currentHtmlContent != null && selectedSource == METEOGALICIA) {
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
        if (networkObserver != null) {
            networkObserver.close();
            networkObserver = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        ++requestGeneration;
        if (wolframRepository != null) wolframRepository.shutdown();
        if (forecastRepository != null) {
            forecastRepository.shutdown();
        }
        super.onDestroy();
    }

    private void updateNetworkStateDisplay(ConnectivityAndInternetAccess.NetworkState state) {
        // El observer es pasivo y puede entregar un snapshot inmediatamente anterior
        // a una caída. Confirmar siempre el estado actual antes de mostrar verde.
        if (state == null || !canStartRemoteRequest()) {
            tvEstadoConexion.setText(R.string.conexion_sen_conexion);
            tvEstadoConexion.setTextColor(0xFFB3261E);
            return;
        }

        if (state.isCaptivePortalDetected()) {
            tvEstadoConexion.setText(R.string.conexion_portal_cautivo);
            tvEstadoConexion.setTextColor(0xFFB06000);
            return;
        }

        if (state.isInternetValidated()) {
            tvEstadoConexion.setText(R.string.conexion_conectado);
            tvEstadoConexion.setTextColor(0xFF188038);
        } else {
            // The passive snapshot does not generate DNS/HTTP traffic. The
            // MeteoGalicia probe remains explicit in loadForecast().
            tvEstadoConexion.setText(R.string.conexion_disponible_sen_validar);
            tvEstadoConexion.setTextColor(0xFFB06000);
        }
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
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putInt(KEY_SELECTED_MUNICIPALITY_INDEX, selectedMunicipalityIndex).apply();
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
        final int generation = ++requestGeneration;
        if (selectedSource == WOLFRAM) {
            loadWolframForecast(municipality, generation);
            return;
        }
        if (!canStartRemoteRequest()) {
            Toast.makeText(this, R.string.error_sen_conexion, Toast.LENGTH_SHORT).show();
            showNetworkFailureDialog(municipality, getString(R.string.error_sen_conexion));
            return;
        }

        btnSeleccionaLocalidad.setEnabled(false);
        btnSeleccionaLocalidad.setText(getString(R.string.cargando_municipio, municipality.getName()));

        // La petición real al backend es el primer chequeo específico; no se duplica
        // con un health check previo que podría quedar obsoleto inmediatamente.
        forecastRepository.getForecast(MainActivity.this, municipality, new ForecastRepository.ForecastCallback() {
                @Override
                public void onSuccess(Forecast forecast, String htmlContent) {
                    if (generation != requestGeneration || isFinishing() || isDestroyed()) return;
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
                    if (generation != requestGeneration || isFinishing() || isDestroyed()) return;
                    android.util.Log.e("MainActivity", "Error fetching forecast from MeteoGalicia", exception);
                    btnSeleccionaLocalidad.setEnabled(true);
                    btnSeleccionaLocalidad.setText(municipality.getName());
                    if (exception instanceof ForecastRepository.NetworkFailureException) {
                        ForecastRepository.NetworkFailureException failure =
                                (ForecastRepository.NetworkFailureException) exception;
                        int message = failure.isCaptivePortal()
                                ? R.string.error_portal_cautivo
                                : (failure.isInternetReachable()
                                ? R.string.error_backend_no_disponible
                                : R.string.error_internet_no_disponible);
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                        showNetworkFailureDialog(municipality, getString(message));
                    } else if (exception instanceof MeteoGaliciaApiClient.BackendHttpException) {
                        Toast.makeText(MainActivity.this,
                                R.string.error_backend_no_disponible, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this,
                                R.string.error_obtenendo_pronostico, Toast.LENGTH_LONG).show();
                    }
                }
            });
    }


    private void updateSourceLabel() {
        btnSeleccionaFuente.setText(selectedSource == METEOGALICIA
                ? R.string.fuente_meteogalicia : R.string.fuente_wolfram);
    }

    private void openSourceSelector() {
        String[] choices = {getString(R.string.fuente_meteogalicia),
                getString(R.string.fuente_wolfram)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_seleccionar_fuente)
                .setSingleChoiceItems(choices, selectedSource, (dialog, choice) -> {
                    dialog.dismiss();
                    if (choice != selectedSource) {
                        requestGeneration++;
                        currentHtmlContent = null;
                        selectedSource = choice;
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                .putInt(KEY_SELECTED_SOURCE, selectedSource).apply();
                        updateSourceLabel();
                        webvPronostico.loadDataWithBaseURL(null, "", "text/html", "UTF-8", null);
                        btnSeleccionaLocalidad.setEnabled(true);
                        btnSeleccionaFuente.setEnabled(true);
                        if (selectedMunicipalityIndex >= 0 && selectedMunicipalityIndex < municipalities.size())
                            btnSeleccionaLocalidad.setText(municipalities.get(selectedMunicipalityIndex).getName());
                    }
                    if (selectedSource == WOLFRAM && getAppId().isEmpty()) {
                        showAppIdDialog();
                    } else if (selectedMunicipalityIndex >= 0
                            && selectedMunicipalityIndex < municipalities.size()) {
                        loadForecast(municipalities.get(selectedMunicipalityIndex));
                    }
                })
                .setNegativeButton(R.string.boton_cancelar, null).show();
    }

    private String getAppId() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getString(APP_ID_KEY, "").trim();
    }

    private void showAppIdDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        input.setHint(R.string.wolfram_app_id_hint);
        input.setText(getAppId());
        int inset = (int) (24 * getResources().getDisplayMetrics().density);
        android.widget.LinearLayout wrapper = new android.widget.LinearLayout(this);
        wrapper.setPadding(inset, 8, inset, 8);
        wrapper.addView(input);
        new AlertDialog.Builder(this)
                .setTitle(R.string.wolfram_configurar)
                .setMessage(R.string.wolfram_app_id_explicacion)
                .setView(wrapper)
                .setPositiveButton(R.string.boton_guardar, (d, w) -> {
                    String id = input.getText().toString().trim();
                    if (id.isEmpty()) {
                        Toast.makeText(this, R.string.wolfram_app_id_requerido,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putString(APP_ID_KEY, id).apply();
                    if (selectedSource == WOLFRAM && selectedMunicipalityIndex >= 0
                            && selectedMunicipalityIndex < municipalities.size())
                        loadForecast(municipalities.get(selectedMunicipalityIndex));
                })
                .setNeutralButton(R.string.wolfram_portal, (d, w) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://developer.wolframalpha.com/portal/myapps/"));
                    if (intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
                })
                .setNegativeButton(R.string.boton_cancelar, null).show();
    }

    private void loadWolframForecast(Municipality municipality, int generation) {
        String id = getAppId();
        if (id.isEmpty()) {
            showAppIdDialog();
            return;
        }
        if (!canStartRemoteRequest()) {
            showWolframNetworkFailureDialog(municipality, getString(R.string.error_sen_conexion));
            return;
        }
        btnSeleccionaLocalidad.setEnabled(false);
        btnSeleccionaFuente.setEnabled(false);
        btnSeleccionaLocalidad.setText(getString(R.string.cargando_municipio, municipality.getName()));
        wolframRepository.getForecast(this, municipality, id, new WolframAlphaRepository.Callback() {
            @Override public void onSuccess(String html) {
                if (generation != requestGeneration || isFinishing() || isDestroyed()) return;
                currentHtmlContent = html;
                btnSeleccionaLocalidad.setEnabled(true);
                btnSeleccionaFuente.setEnabled(true);
                btnSeleccionaLocalidad.setText(municipality.getName());
                webvPronostico.loadDataWithBaseURL("https://api.wolframalpha.com/",
                        html, "text/html", "UTF-8", null);
                // Notifications and widgets remain exclusively MeteoGalicia-backed.
            }

            @Override public void onError(Exception exception) {
                if (generation != requestGeneration || isFinishing() || isDestroyed()) return;
                btnSeleccionaLocalidad.setEnabled(true);
                btnSeleccionaFuente.setEnabled(true);
                btnSeleccionaLocalidad.setText(municipality.getName());
                if (exception instanceof ForecastRepository.NetworkFailureException) {
                    ForecastRepository.NetworkFailureException e =
                            (ForecastRepository.NetworkFailureException) exception;
                    int message = e.isCaptivePortal() ? R.string.error_portal_cautivo
                            : e.isInternetReachable() ? R.string.error_backend_no_disponible
                            : R.string.error_internet_no_disponible;
                    showWolframNetworkFailureDialog(municipality, getString(message));
                } else if (exception instanceof WolframAlphaApiClient.ApiHttpException
                        && ((((WolframAlphaApiClient.ApiHttpException) exception).getStatus() == 401)
                        || (((WolframAlphaApiClient.ApiHttpException) exception).getStatus() == 403))) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(R.string.wolfram_app_id_invalido)
                            .setPositiveButton(R.string.wolfram_configurar,
                                    (d, w) -> showAppIdDialog())
                            .setNegativeButton(R.string.boton_cancelar, null).show();
                } else {
                    android.util.Log.w("MainActivity", "Wolfram forecast failed", exception);
                    Toast.makeText(MainActivity.this, R.string.wolfram_fallo,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showWolframNetworkFailureDialog(Municipality municipality, String message) {
        WeatherNotificationHelper.showNetworkAlertNotification(this, message);
        new AlertDialog.Builder(this).setTitle(R.string.atencion_titulo)
                .setMessage(message)
                .setPositiveButton(R.string.boton_reintentar, (d, w) -> loadForecast(municipality))
                .setNegativeButton(R.string.boton_diagnostico,
                        (d, w) -> openNetworkDiagnostics()).show();
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

        forecastRepository.getForecast(this, municipality, new ForecastRepository.ForecastCallback() {
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

    private boolean canStartRemoteRequest() {
        return NetworkValidationManager.canStartRemoteRequest(this);
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
