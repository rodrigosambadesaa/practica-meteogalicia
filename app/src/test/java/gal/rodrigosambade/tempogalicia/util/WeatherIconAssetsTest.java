package gal.rodrigosambade.tempogalicia.util;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class WeatherIconAssetsTest {
    private static String packagedSvg(String name) throws java.io.IOException {
        File icon = new File("src/main/assets/meteocons/" + name + ".svg");
        return new String(Files.readAllBytes(icon.toPath()), StandardCharsets.UTF_8);
    }

    @Test public void inlineSvgReplacesPlaceholdersInWeatherForecastHtml() {
        String html = "<section>"
                + WeatherIcons.htmlIcon("clear-day")
                + WeatherIcons.htmlIcon("rain")
                + "</section>";
        String output = WeatherIconAssets.inlineIcons(html, WeatherIconAssetsTest::packagedSvg);
        assertEquals(2, count(output, "<svg class=\"weather-symbol\""));
        assertFalse(output.contains("weather-icon://"));
        assertFalse(output.contains("data:image/"));
        assertTrue(output.contains("aria-label=\"Ilustración meteorológica\""));
        assertTrue(output.contains("wx0-a"));
        assertTrue(output.contains("wx1-a"));
        assertTrue(output.contains("url(#wx0-a)"));
        assertTrue(output.contains("url(#wx1-a)"));
    }

    @Test public void multipleInstancesOfSameIconUseIndependentSvgIds() {
        String html = WeatherIcons.htmlIcon("clear-day") + WeatherIcons.htmlIcon("clear-day");
        String output = WeatherIconAssets.inlineIcons(html, WeatherIconAssetsTest::packagedSvg);
        assertEquals(2, count(output, "<svg class=\"weather-symbol\""));
        assertTrue(output.contains("id=\"wx0-a\""));
        assertTrue(output.contains("id=\"wx1-a\""));
        assertTrue(output.contains("xlink:href=\"#wx0-b\""));
        assertTrue(output.contains("xlink:href=\"#wx1-b\""));
    }

    @Test public void unknownConditionsRenderBundledNotAvailableSymbol() {
        String output = WeatherIconAssets.inlineIcons(WeatherIcons.htmlIcon("not-available"),
                WeatherIconAssetsTest::packagedSvg);
        assertTrue(output.contains("aria-label=\"Estado meteorológico no especificado\""));
        assertTrue(output.contains("<svg"));
    }

    @Test public void brokenAssetStillRendersVisibleFallbackWithoutHttpRequests() {
        String output = WeatherIconAssets.inlineIcons(WeatherIcons.htmlIcon("rain"),
                name -> { throw new java.io.IOException("missing"); });
        assertTrue(output.contains("aria-label=\"Icono no disponible\""));
        assertFalse(output.contains("weather-icon://"));
        assertFalse(output.contains("http://"));
        assertFalse(output.contains("https://"));
    }

    @Test public void untouchedHtmlOrNullRemainsUntouched() {
        assertNull(WeatherIconAssets.inlineIcons(null, WeatherIconAssetsTest::packagedSvg));
        assertEquals("<h1>Sin datos</h1>",
                WeatherIconAssets.inlineIcons("<h1>Sin datos</h1>", WeatherIconAssetsTest::packagedSvg));
    }

    private static int count(String source, String value) {
        int n = 0, offset = 0;
        while ((offset = source.indexOf(value, offset)) >= 0) {
            n++;
            offset += value.length();
        }
        return n;
    }
}
