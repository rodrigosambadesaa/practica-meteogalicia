package gal.rodrigosambade.tempogalicia.util;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inserts trusted, bundled Meteocons SVG markup directly in forecast HTML.
 *
 * An SVG loaded as an image data URI can be suppressed by WebView image
 * loading/restrictions. Inline SVG keeps icons in the document (offline, with
 * JavaScript and file access disabled). Every SVG gets unique element IDs so
 * gradients/symbol references in multiple icons cannot collide.
 */
public final class WeatherIconAssets {
    private static final Pattern ICON_IMG = Pattern.compile(
            "<img\\b[^>]*\\bsrc=\"weather-icon://([a-z-]+)\"[^>]*>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_REFERENCE = Pattern.compile(
            "id=\"([A-Za-z0-9_-]+)\"|url\\(#([A-Za-z0-9_-]+)\\)|((?:xlink:)?href)=\"#([A-Za-z0-9_-]+)\"");
    private static final String MISSING_ICON =
            "<span class=\"weather-symbol\" role=\"img\" aria-label=\"Icono no disponible\">🌦️</span>";

    interface IconSource {
        String read(String name) throws IOException;
    }

    private WeatherIconAssets() { }

    public static String inlineIcons(Context context, String html) {
        return inlineIcons(html, name -> {
            try (InputStream stream = context.getAssets().open("meteocons/" + name + ".svg");
                 ByteArrayOutputStream data = new ByteArrayOutputStream()) {
                byte[] bytes = new byte[4096];
                int read;
                while ((read = stream.read(bytes)) != -1) {
                    if (data.size() + read > 100_000) {
                        throw new IOException("Weather icon asset is too large");
                    }
                    data.write(bytes, 0, read);
                }
                return new String(data.toByteArray(), StandardCharsets.UTF_8);
            }
        });
    }

    // Pure Java seam for regression tests (no Android WebView or AppID required).
    static String inlineIcons(String html, IconSource source) {
        if (html == null || !html.contains("weather-icon://")) return html;
        Matcher matcher = ICON_IMG.matcher(html);
        StringBuffer result = new StringBuffer(html.length() + 4096);
        int instance = 0;
        while (matcher.find()) {
            String name = matcher.group(1);
            String replacement = MISSING_ICON;
            if (WeatherIcons.isBundled(name)) {
                try {
                    replacement = makeInlineSvg(source.read(name), instance, name);
                } catch (IOException exception) {
                    // Keep a visible, accessible fallback if an asset is missing.
                    replacement = MISSING_ICON;
                }
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            instance++;
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String makeInlineSvg(String xml, int instance, String iconName) {
        if (xml == null || !xml.contains("<svg ") || xml.contains("<script")
                || xml.contains("<foreignObject") || xml.contains("<!DOCTYPE")) {
            return MISSING_ICON;
        }
        String prefix = "wx" + instance + "-";
        Matcher refs = SVG_REFERENCE.matcher(xml);
        StringBuffer unique = new StringBuffer(xml.length());
        while (refs.find()) {
            String replacement;
            if (refs.group(1) != null) {
                replacement = "id=\"" + prefix + refs.group(1) + "\"";
            } else if (refs.group(2) != null) {
                replacement = "url(#" + prefix + refs.group(2) + ")";
            } else {
                replacement = refs.group(3) + "=\"#" + prefix + refs.group(4) + "\"";
            }
            refs.appendReplacement(unique, Matcher.quoteReplacement(replacement));
        }
        refs.appendTail(unique);
        String label = "not-available".equals(iconName)
                ? "Estado meteorológico no especificado"
                : "Ilustración meteorológica";
        return unique.toString().replaceFirst("<svg\\b",
                "<svg class=\"weather-symbol\" role=\"img\" aria-label=\"" + label + "\"");
    }
}
