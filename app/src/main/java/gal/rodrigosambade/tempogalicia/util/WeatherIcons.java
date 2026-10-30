package gal.rodrigosambade.tempogalicia.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Maps meteorological evidence to a locally bundled Meteocons illustration.
 * Icon names are an allowlist, never derived from raw HTML or remote URLs.
 * Source: https://github.com/rodrigosambadesaa/weather-icons (MIT / Bas Milius).
 */
public final class WeatherIcons {
    private static final Set<String> KNOWN = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("clear-day", "partly-cloudy-day", "partly-cloudy-day-rain",
                    "cloudy", "overcast", "rain", "drizzle", "thunderstorms",
                    "fog", "snow", "hail", "wind", "sleet", "not-available")));

    private WeatherIcons() { }

    /** Approximation from rain risk, not an observation of sunshine or cloud cover. */
    public static String forRainProbability(int percentage) {
        if (percentage >= 70) return "rain";
        if (percentage >= 40) return "partly-cloudy-day-rain";
        if (percentage >= 20) return "partly-cloudy-day";
        return "clear-day";
    }

    /** A condition is unknown when the textual response does not mention one. */
    public static String forConditions(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return null;
        String text = " " + rawText.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}]+", " ") + " ";
        if (contains(text, "thunderstorm", "thunderstorms", "lightning", "tormenta", "tormentas")) return "thunderstorms";
        if (contains(text, "hail", "granizo")) return "hail";
        if (contains(text, "sleet", "aguanieve", "freezing rain")) return "sleet";
        if (contains(text, "snow", "snowfall", "nieve", "nevando")) return "snow";
        if (contains(text, "drizzle", "llovizna")) return "drizzle";
        if (contains(text, "rain", "rainy", "showers", "lluvia", "chubascos")) return "rain";
        if (contains(text, "fog", "mist", "niebla", "neblina")) return "fog";
        if (contains(text, "overcast", "cubierto")) return "overcast";
        if (contains(text, "partly cloudy", "partly sunny", "partially cloudy",
                "parcialmente nuboso", "parcialmente soleado")) return "partly-cloudy-day";
        if (contains(text, "cloudy", "clouds", "nuboso", "nubes")) return "cloudy";
        if (contains(text, "clear", "sunny", "soleado", "despejado")) return "clear-day";
        if (contains(text, "windy", "ventoso", "wind gusts", "rachas de viento")) return "wind";
        return null;
    }

    private static boolean contains(String haystack, String... keywords) {
        for (String word : keywords) {
            if (haystack.contains(" " + word + " ")) return true;
        }
        return false;
    }

    public static boolean isBundled(String name) {
        return KNOWN.contains(name);
    }

    /** A placeholder that is replaced with a trusted, inlined SVG before WebView display. */
    public static String htmlIcon(String icon) {
        if (icon == null || !isBundled(icon)) return "";
        return "<img class=\"weather-symbol\" src=\"weather-icon://" + icon
                + "\" alt=\"Ilustración meteorológica orientativa\" loading=\"lazy\" />";
    }
}
