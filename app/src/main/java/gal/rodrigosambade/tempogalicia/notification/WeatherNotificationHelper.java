package gal.rodrigosambade.tempogalicia.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import gal.rodrigosambade.tempogalicia.R;
import gal.rodrigosambade.tempogalicia.model.Forecast;
import gal.rodrigosambade.tempogalicia.model.ForecastDay;
import gal.rodrigosambade.tempogalicia.model.Municipality;
import gal.rodrigosambade.tempogalicia.ui.MainActivity;
import gal.rodrigosambade.tempogalicia.ui.NetworkDiagnosticsActivity;

public final class WeatherNotificationHelper {

    public static final String CHANNEL_ID_WEATHER = "tempo_galicia_weather_channel";
    public static final String CHANNEL_ID_NETWORK = "tempo_galicia_network_channel";

    private static final int NOTIFICATION_ID_FORECAST = 1001;
    private static final int NOTIFICATION_ID_RAIN_ALERT = 1002;
    private static final int NOTIFICATION_ID_NETWORK = 1003;

    private WeatherNotificationHelper() {
        // Clase de utilidades
    }

    /**
     * Crea e rexistra as canles de notificación necesarias para Android 8.0+ (API 26+).
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            // Canle de tempo e alertas
            CharSequence nameWeather = context.getString(R.string.canal_notificacion_tempo_nome);
            String descriptionWeather = context.getString(R.string.canal_notificacion_tempo_desc);
            NotificationChannel channelWeather = new NotificationChannel(
                    CHANNEL_ID_WEATHER,
                    nameWeather,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channelWeather.setDescription(descriptionWeather);
            manager.createNotificationChannel(channelWeather);

            // Canle de diagnóstico de rede
            CharSequence nameNetwork = context.getString(R.string.canal_notificacion_rede_nome);
            String descriptionNetwork = context.getString(R.string.canal_notificacion_rede_desc);
            NotificationChannel channelNetwork = new NotificationChannel(
                    CHANNEL_ID_NETWORK,
                    nameNetwork,
                    NotificationManager.IMPORTANCE_LOW);
            channelNetwork.setDescription(descriptionNetwork);
            manager.createNotificationChannel(channelNetwork);
        }
    }

    /**
     * Mostra unha notificación coa previsión do tempo para un municipio.
     */
    public static void showWeatherForecastNotification(Context context, Municipality municipality, Forecast forecast) {
        if (context == null || municipality == null || forecast == null) return;

        createNotificationChannels(context);

        ForecastDay today = forecast.getDays() != null && !forecast.getDays().isEmpty()
                ? forecast.getDays().get(0)
                : null;

        String title = context.getString(R.string.notificacion_titulo_tempo, municipality.getName());
        String body;
        if (today != null) {
            body = context.getString(R.string.notificacion_corpo_tempo,
                    today.getMaxTemperature(),
                    today.getMinTemperature(),
                    today.getRainProbability());
        } else {
            body = municipality.getName();
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_WEATHER)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_FORECAST, builder.build());
        } catch (SecurityException ignored) {
            // Permiso POST_NOTIFICATIONS non concedido no dispositivo
        }

        if (today != null && today.getRainProbability() >= 50) {
            showRainAlertNotification(context, municipality, today.getRainProbability());
        }
    }

    /**
     * Mostra unha alerta meteorolóxica de alta probabilidade de choiva.
     */
    public static void showRainAlertNotification(Context context, Municipality municipality, int rainProbability) {
        if (context == null || municipality == null) return;

        createNotificationChannels(context);

        String title = context.getString(R.string.alerta_choiva_titulo, municipality.getName());
        String body = context.getString(R.string.alerta_choiva_corpo, rainProbability);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_WEATHER)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_RAIN_ALERT, builder.build());
        } catch (SecurityException ignored) {
            // Permiso POST_NOTIFICATIONS non concedido
        }
    }

    /**
     * Mostra unha notificación de estado de rede ou inalcanzabilidade de dominios.
     */
    public static void showNetworkAlertNotification(Context context, String reasonMessage) {
        if (context == null) return;

        createNotificationChannels(context);

        String title = context.getString(R.string.notificacion_rede_titulo);
        String body = reasonMessage != null ? reasonMessage : context.getString(R.string.conexion_dominios_inalcanzables);

        Intent intent = new Intent(context, NetworkDiagnosticsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_NETWORK)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_NETWORK, builder.build());
        } catch (SecurityException ignored) {
            // Permiso POST_NOTIFICATIONS non concedido
        }
    }
}
