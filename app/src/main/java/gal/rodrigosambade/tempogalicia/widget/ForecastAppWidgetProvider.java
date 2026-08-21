package gal.rodrigosambade.tempogalicia.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import gal.rodrigosambade.tempogalicia.R;
import gal.rodrigosambade.tempogalicia.model.Forecast;
import gal.rodrigosambade.tempogalicia.model.ForecastDay;
import gal.rodrigosambade.tempogalicia.model.Municipality;
import gal.rodrigosambade.tempogalicia.repository.ForecastRepository;
import gal.rodrigosambade.tempogalicia.ui.MainActivity;
import gal.rodrigosambade.tempogalicia.util.NetworkValidationManager;

import java.util.List;

public class ForecastAppWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH_WIDGET = "gal.rodrigosambade.tempogalicia.ACTION_REFRESH_WIDGET";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH_WIDGET.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, ForecastAppWidgetProvider.class);
            int[] appWidgetIds = manager.getAppWidgetIds(thisWidget);
            onUpdate(context, manager, appWidgetIds);
        }
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager != null) {
            ComponentName thisWidget = new ComponentName(context, ForecastAppWidgetProvider.class);
            int[] appWidgetIds = manager.getAppWidgetIds(thisWidget);
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, manager, appWidgetId);
            }
        }
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_forecast);

        // Premer no widget abre MainActivity
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );
        views.setOnClickPendingIntent(R.id.widget_container, mainPendingIntent);

        // Premer no botón de refresco envía broadcast de actualización
        Intent refreshIntent = new Intent(context, ForecastAppWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH_WIDGET);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );
        views.setOnClickPendingIntent(R.id.ibtn_widget_refresh, refreshPendingIntent);

        // Cargar datos do municipio por defecto (A Coruña)
        Municipality defaultMunicipality = Municipality.getDefaultMunicipalities().get(0);
        views.setTextViewText(R.id.tv_widget_municipality, defaultMunicipality.getName());

        // Comprobación asíncrona de conectividade cos dominios de MeteoGalicia
        NetworkValidationManager.checkMeteoGaliciaDomainAsync(context, result -> {
            boolean reachable = result != null && result.isReachable();
            if (reachable) {
                views.setTextViewText(R.id.tv_widget_status, "Conectado a MeteoGalicia");
                views.setTextColor(R.id.tv_widget_status, 0xFFB9F6CA);
            } else {
                views.setTextViewText(R.id.tv_widget_status, "Datos offline (Sen conexión)");
                views.setTextColor(R.id.tv_widget_status, 0xFFFFCDD2);
            }

            ForecastRepository repository = new ForecastRepository();
            repository.getForecast(defaultMunicipality, new ForecastRepository.ForecastCallback() {
                @Override
                public void onSuccess(Forecast forecast, String htmlContent) {
                    List<ForecastDay> days = forecast.getDays();
                    if (days != null && !days.isEmpty()) {
                        ForecastDay today = days.get(0);
                        views.setTextViewText(R.id.tv_widget_temperatures,
                                "Máx: " + today.getMaxTemperature() + "°C | Mín: " + today.getMinTemperature() + "°C");
                        views.setTextViewText(R.id.tv_widget_rain,
                                "Prob. Choiva: " + today.getRainProbability() + "%");
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                    repository.shutdown();
                }

                @Override
                public void onError(Exception exception) {
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                    repository.shutdown();
                }
            });
        });

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
