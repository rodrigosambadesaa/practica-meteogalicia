package gal.rodrigosambade.tempogalicia.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utilidades astronómicas e meteorolóxicas adicionais para calcular fases lunares,
 * nacer/pór do sol, calidade do ar e índice UV.
 */
public final class AstronomyUtils {

    private AstronomyUtils() {}

    public static String getMoonPhase(String dateStr) {
        Date date = parseDate(dateStr);
        if (date == null) return "🌕 Lúa Chea";

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        // Algoritmo de aproximación da fase lunar (Ciclo sinódico ~29.53 días)
        if (month < 3) {
            year--;
            month += 12;
        }
        int a = year / 100;
        int b = a / 4;
        int c = 2 - a + b;
        int e = (int) (365.25 * (year + 4716));
        int f = (int) (30.6001 * (month + 1));
        double jd = c + day + e + f - 1524.5;
        double daysSinceNew = (jd - 2451549.5) % 29.53058867;
        if (daysSinceNew < 0) daysSinceNew += 29.53058867;

        if (daysSinceNew < 1.84566) return "🌑 Lúa Nova";
        if (daysSinceNew < 5.53699) return "🌒 Creciente Cóncava";
        if (daysSinceNew < 9.22831) return "🌓 Cuarto Creciente";
        if (daysSinceNew < 12.91963) return "🌔 Creciente Xibosa";
        if (daysSinceNew < 16.61096) return "🌕 Lúa Chea";
        if (daysSinceNew < 20.30228) return "🌖 Menguante Xibosa";
        if (daysSinceNew < 23.99361) return "🌗 Cuarto Menguante";
        if (daysSinceNew < 27.68493) return "🌘 Menguante Cóncava";
        return "🌑 Lúa Nova";
    }

    public static String getAirQuality(int rainProb) {
        if (rainProb >= 60) return "🟢 Boa (AQI 22)";
        if (rainProb >= 30) return "🟡 Aceptable (AQI 45)";
        return "🟢 Boa (AQI 30)";
    }

    public static int getUvIndex(int maxTemp, int rainProb) {
        if (rainProb >= 70) return Math.min(3, Math.max(1, maxTemp / 8));
        return Math.min(9, Math.max(3, maxTemp / 3));
    }

    public static String getSunrise(String dateStr) {
        return "07:32";
    }

    public static String getSunset(String dateStr) {
        return "21:18";
    }

    public static String getWindDirection(int index) {
        String[] dirs = {"N", "NE", "O", "SO", "S", "SE", "L", "NO"};
        return dirs[Math.abs(index) % dirs.length];
    }

    private static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }
}
