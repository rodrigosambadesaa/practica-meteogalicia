package gal.rodrigosambade.tempogalicia.util;

import android.content.Context;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Embeds bundled SVGs in forecast HTML as data URIs. This avoids additional
 * requests to third-party servers and avoids enabling WebView file access.
 */
public final class WeatherIconAssets {
    private static final Pattern PLACEHOLDER =
            Pattern.compile("src=\\\"weather-icon://([a-z-]+)\\\"");
    private static final String FALLBACK =
            "src=\\\"data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%201%201'/%3E\\\"";

    private WeatherIconAssets() { }

    public static String inlineIcons(Context context, String html) {
        if (html == null || html.indexOf("weather-icon://") < 0) return html;
        Matcher matcher = PLACEHOLDER.matcher(html);
        StringBuffer result = new StringBuffer(html.length() + 4096);
        Map<String, String> resolved = new HashMap<>();
        while (matcher.find()) {
            String name = matcher.group(1);
            String replacement = resolved.get(name);
            if (replacement == null) {
                replacement = resolve(context, name);
                resolved.put(name, replacement);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String resolve(Context context, String name) {
        if (!WeatherIcons.isBundled(name)) return FALLBACK;
        try (InputStream stream = context.getAssets().open("meteocons/" + name + ".svg");
             ByteArrayOutputStream data = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[4096];
            int read;
            while ((read = stream.read(bytes)) != -1) {
                data.write(bytes, 0, read);
            }
            return "src=\\\"data:image/svg+xml;base64,"
                    + Base64.encodeToString(data.toByteArray(), Base64.NO_WRAP) + "\\\"";
        } catch (IOException e) {
            android.util.Log.w("WeatherIconAssets", "Missing bundled icon: " + name, e);
            return FALLBACK;
        }
    }
}
