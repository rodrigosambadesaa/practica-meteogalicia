package gal.rodrigosambade.tempogalicia;

import org.junit.Test;
import static org.junit.Assert.*;

import gal.rodrigosambade.tempogalicia.api.WolframAlphaApiClient;
import gal.rodrigosambade.tempogalicia.model.Municipality;
import gal.rodrigosambade.tempogalicia.parser.WolframAlphaWeatherParser;

public class WolframAlphaWeatherParserTest {
    private final WolframAlphaWeatherParser parser = new WolframAlphaWeatherParser();

    @Test public void queryUsesChosenMunicipalityNotHardcodedSantiago() {
        assertEquals("weather Cee", WolframAlphaApiClient.buildQuery(new Municipality("Cee", "15023")));
        assertTrue(WolframAlphaApiClient.buildRequestUrl(new Municipality("A Coruña", "15030"), "test")
                .contains("weather+A+Coru%C3%B1a"));
    }

    @Test public void rendersEnglishWeatherPodsAsEscapedSpanishHtml() throws Exception {
        String json = "{\"queryresult\":{\"success\":true,\"error\":false,\"pods\":["
                + "{\"id\":\"Input\",\"subpods\":[{\"plaintext\":\"weather\"}]},"
                + "{\"id\":\"InstantaneousWeather:WeatherData\",\"subpods\":[{\"plaintext\":\"partly cloudy | 18 degrees celsius | wind speed 12 km/h | <script>\"}]},"
                + "{\"id\":\"WeatherForecast:WeatherData\",\"subpods\":[{\"plaintext\":\"tomorrow light rain\"}]}"
                + "]}}";
        String html = parser.render(json, "Santiago de Compostela");
        assertTrue(html.contains("parcialmente nuboso"));
        assertTrue(html.contains("grados Celsius"));
        assertTrue(html.contains("mañana lluvia ligera"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("Previsión meteorológica"));
        assertFalse(html.contains("<h2>weather</h2>"));
    }

    @Test public void handlesOnePodAndOneSubpodObjects() throws Exception {
        String html = parser.render("{\"queryresult\":{\"success\":true,\"pods\":{\"id\":\"WeatherForecast:WeatherData\",\"subpods\":{\"plaintext\":\"sunny\"}}}}", "Cee");
        assertTrue(html.contains("soleado"));
    }

    @Test(expected = org.json.JSONException.class)
    public void rejectsApiFailure() throws Exception {
        parser.render("{\"queryresult\":{\"success\":false}}", "Cee");
    }

    @Test public void encodesHtmlInSelectedMunicipality() throws Exception {
        assertEquals("Cee &amp; &lt;Vigo&gt;", WolframAlphaWeatherParser.escapeHtml("Cee & <Vigo>"));
    }
}
