package gal.rodrigosambade.tempogalicia.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import gal.rodrigosambade.tempogalicia.R;
import gal.rodrigosambade.tempogalicia.model.Forecast;
import gal.rodrigosambade.tempogalicia.model.Municipality;
import gal.rodrigosambade.tempogalicia.repository.ForecastRepository;

import net.i2p.android.router.util.ConnectivityAndInternetAccess;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String OFFICIAL_METEOGALICIA_PHONE = "881999654";

    private Button btnSeleccionaLocalidad;
    private ImageButton ibtnTelefono;
    private ImageButton ibtnInfo;
    private WebView webvPronostico;
    private TextView tvEstadoConexion;

    private ConnectivityAndInternetAccess.NetworkObserver networkObserver;
    private ForecastRepository forecastRepository;

    private int selectedMunicipalityIndex = -1;
    private List<Municipality> municipalities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        municipalities = Municipality.getDefaultMunicipalities();
        forecastRepository = new ForecastRepository();

        btnSeleccionaLocalidad = findViewById(R.id.btn_seleccionaLocalidad);
        webvPronostico = findViewById(R.id.webv_pronostico);
        ibtnTelefono = findViewById(R.id.ibtn_telefono);
        ibtnInfo = findViewById(R.id.ibtn_info);
        tvEstadoConexion = findViewById(R.id.tv_estadoConexion);

        webvPronostico.getSettings().setJavaScriptEnabled(false);
        webvPronostico.getSettings().setBuiltInZoomControls(true);
        webvPronostico.getSettings().setDisplayZoomControls(false);

        btnSeleccionaLocalidad.setOnClickListener(view -> openMunicipalitySelector());
        ibtnTelefono.setOnClickListener(view -> dialOfficialContactPhone());
        if (ibtnInfo != null) {
            ibtnInfo.setOnClickListener(view -> showAboutDialog());
        }
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
        if (forecastRepository != null) {
            forecastRepository.shutdown();
        }
        super.onDestroy();
    }

    private void updateNetworkStateDisplay(ConnectivityAndInternetAccess.NetworkState state) {
        if (state != null && state.isConnected() && state.isInternetValidated()) {
            tvEstadoConexion.setText(R.string.conexion_conectado);
            tvEstadoConexion.setTextColor(0xFF188038);
        } else if (state != null && state.isConnected()) {
            tvEstadoConexion.setText(R.string.conexion_disponible_sen_validar);
            tvEstadoConexion.setTextColor(0xFFB06000);
        } else {
            tvEstadoConexion.setText(R.string.conexion_sen_conexion);
            tvEstadoConexion.setTextColor(0xFFB3261E);
        }
    }

    private void openMunicipalitySelector() {
        String[] names = new String[municipalities.size()];
        for (int i = 0; i < municipalities.size(); i++) {
            names[i] = municipalities.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_seleccionar_municipio)
                .setSingleChoiceItems(names, selectedMunicipalityIndex, (dialog, which) -> {
                    selectedMunicipalityIndex = which;
                    dialog.dismiss();
                    loadForecast(municipalities.get(which));
                })
                .setNegativeButton(R.string.boton_cancelar, null)
                .show();
    }

    private void loadForecast(Municipality municipality) {
        if (!ConnectivityAndInternetAccess.isConnected(this)) {
            Toast.makeText(this, R.string.error_sen_conexion, Toast.LENGTH_SHORT).show();
            return;
        }

        btnSeleccionaLocalidad.setEnabled(false);
        btnSeleccionaLocalidad.setText(getString(R.string.cargando_municipio, municipality.getName()));

        forecastRepository.getForecast(municipality, new ForecastRepository.ForecastCallback() {
            @Override
            public void onSuccess(Forecast forecast, String htmlContent) {
                btnSeleccionaLocalidad.setEnabled(true);
                btnSeleccionaLocalidad.setText(municipality.getName());
                webvPronostico.loadDataWithBaseURL(
                        "https://servizos.meteogalicia.gal/",
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null);
            }

            @Override
            public void onError(Exception exception) {
                android.util.Log.e("MainActivity", "Error fetching forecast from MeteoGalicia", exception);
                btnSeleccionaLocalidad.setEnabled(true);
                btnSeleccionaLocalidad.setText(municipality.getName());
                Toast.makeText(MainActivity.this, R.string.error_obtenendo_pronostico, Toast.LENGTH_LONG).show();
            }
        });
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
