package gal.rodrigosambade.tempogalicia;

import gal.rodrigosambade.tempogalicia.model.Forecast;
import gal.rodrigosambade.tempogalicia.model.ForecastDay;
import gal.rodrigosambade.tempogalicia.model.Municipality;
import gal.rodrigosambade.tempogalicia.parser.ForecastParser;
import gal.rodrigosambade.tempogalicia.parser.WolframAlphaWeatherParser;
import gal.rodrigosambade.tempogalicia.util.WeatherIcons;

import org.junit.Test;
import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.*;

public class WeatherIconsTest {
    @Test public void precipitationRiskMapsToBundledIllustrations() {
        assertEquals("clear-day", WeatherIcons.forRainProbability(0));
        assertEquals("partly-cloudy-day", WeatherIcons.forRainProbability(20));
        assertEquals("partly-cloudy-day-rain", WeatherIcons.forRainProbability(40));
        assertEquals("rain", WeatherIcons.forRainProbability(70));
        assertEquals("rain", WeatherIcons.forRainProbability(100));
    }

    @Test public void forecastRendersLocalIconPlaceholderWithoutExternalRequest() {
        Forecast forecast = new Forecast(new Municipality("Cee", "15023"),
                Arrays.asList(new ForecastDay("2026-10-29", 17, 8, 75, 12, 1, 75)));
        String html = new ForecastParser().renderHtmlForecast(forecast);
        assertTrue(html.contains("src=\"weather-icon://rain\""));
        assertTrue(html.contains("Ilustración meteorológica orientativa"));
        assertFalse(html.contains("raw.githubusercontent.com"));
        assertTrue(html.contains("Fonte dos datos: Xunta de Galicia"));
    }

    @Test public void wolframRenderPicksAnIconOnlyForRecognizedCondition() throws Exception {
        String json = "{\"queryresult\":{\"success\":true,\"pods\":["
                + "{\"id\":\"InstantaneousWeather:WeatherData\",\"subpods\":{\"plaintext\":\"light rain | 12 degrees celsius\"}},"
                + "{\"id\":\"WeatherForecast:WeatherData\",\"subpods\":{\"plaintext\":\"tomorrow partly cloudy\"}},"
                + "{\"id\":\"HistoricalTemperature\",\"subpods\":{\"plaintext\":\"20 degrees celsius\"}}"
                + "]}}";
        String html = new WolframAlphaWeatherParser().render(json, "Vigo");
        assertTrue(html.contains("src=\"weather-icon://rain\""));
        assertTrue(html.contains("src=\"weather-icon://partly-cloudy-day\""));
        assertEquals(2, occurrences(html, "src=\"weather-icon://"));
        assertTrue(html.contains("lluvia ligera"));
    }

    @Test public void unknownTextDoesNotInventConditionsOrAllowPathInjection() {
        assertNull(WeatherIcons.forConditions("temperature | 17 Celsius"));
        assertNull(WeatherIcons.forConditions(null));
        assertNull(WeatherIcons.forConditions(" "));
        assertEquals("", WeatherIcons.htmlIcon("../malicious"));
        assertFalse(WeatherIcons.isBundled("https://test.invalid/"));
        assertEquals("thunderstorms", WeatherIcons.forConditions("heavy rain and thunderstorms"));
        assertEquals("fog", WeatherIcons.forConditions("foggy mist"));
        assertEquals("snow", WeatherIcons.forConditions("snow showers"));
    }

    @Test public void assetsExistForAllMappedWeatherConditions() {
        String[] names = {"clear-day", "partly-cloudy-day", "partly-cloudy-day-rain",
                "cloudy", "overcast", "rain", "drizzle", "thunderstorms", "fog",
                "snow", "hail", "wind", "sleet", "not-available"};
        for (String name : names) {
            assertTrue("Icon is not allowlisted: " + name, WeatherIcons.isBundled(name));
            assertTrue("Missing SVG: " + name, new File("src/main/assets/meteocons/" + name + ".svg").isFile());
        }
        assertTrue(new File("src/main/assets/meteocons/LICENSE").isFile());
    }


    @Test public void wolframWeatherPodWithoutConditionsGetsExplicitUnknownIcon() throws Exception {
        String json = "{\"queryresult\":{\"success\":true,\"pods\":["
                + "{\"id\":\"InstantaneousWeather:WeatherData\",\"title\":\"Latest recorded weather for Cee\","
                + "\"subpods\":{\"plaintext\":\"temperature | 15 degrees celsius | relative humidity | 72%\"}},"
                + "{\"id\":\"HistoricalTemperature:WeatherData\",\"subpods\":{\"plaintext\":\"15 degrees celsius\"}}"
                + "]}}";
        String html = new WolframAlphaWeatherParser().render(json, "Cee");
        assertEquals(1, occurrences(html, "src=\"weather-icon://"));
        assertTrue(html.contains("src=\"weather-icon://not-available\""));
        assertTrue(html.contains("temperatura"));
    }

    @Test public void wolframSubpodMetadataCanSupplyWeatherCondition() throws Exception {
        String json = "{\"queryresult\":{\"success\":true,\"pods\":["
                + "{\"id\":\"WeatherForecast:WeatherData\",\"subpods\":{"
                + "\"title\":\"light snow\", \"plaintext\":\"tomorrow | 2 degrees celsius\"}}]}}";
        String html = new WolframAlphaWeatherParser().render(json, "Cee");
        assertTrue(html.contains("src=\"weather-icon://snow\""));
        assertFalse(html.contains("src=\"weather-icon://not-available\""));
    }

    private static int occurrences(String text, String target) {
        int total = 0, at = 0;
        while ((at = text.indexOf(target, at)) >= 0) {
            total++;
            at += target.length();
        }
        return total;
    }
}
