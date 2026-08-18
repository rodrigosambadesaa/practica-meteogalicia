package gal.rodrigosambade.tempogalicia;

import gal.rodrigosambade.tempogalicia.model.Forecast;
import gal.rodrigosambade.tempogalicia.model.Municipality;
import gal.rodrigosambade.tempogalicia.parser.ForecastParser;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ForecastParserTest {

    private ForecastParser parser;
    private Municipality testMunicipality;

    @Before
    public void setUp() {
        parser = new ForecastParser();
        testMunicipality = new Municipality("Santiago de Compostela", "15078");
    }

    @Test
    public void testParseValidJson() throws JSONException {
        String json = "{\n" +
                "  \"predMPrazo\": {\n" +
                "    \"listaPredDiaMPrazo\": [\n" +
                "      {\n" +
                "        \"dataPredicion\": \"2026-08-18T00:00:00\",\n" +
                "        \"tMax\": 24,\n" +
                "        \"tMin\": 14,\n" +
                "        \"probIcoCeo1\": 10,\n" +
                "        \"probIcoCeo2\": 30,\n" +
                "        \"probIcoCeo3\": 20\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        Forecast forecast = parser.parseJson(testMunicipality, json);
        assertNotNull(forecast);
        assertEquals("Santiago de Compostela", forecast.getMunicipality().getName());
        assertFalse(forecast.isEmpty());
        assertEquals(1, forecast.getDays().size());

        assertEquals("2026-08-18", forecast.getDays().get(0).getDate());
        assertEquals(24, forecast.getDays().get(0).getMaxTemperature());
        assertEquals(14, forecast.getDays().get(0).getMinTemperature());
        assertEquals(30, forecast.getDays().get(0).getRainProbability());
    }

    @Test(expected = JSONException.class)
    public void testParseEmptyJsonThrowsException() throws JSONException {
        parser.parseJson(testMunicipality, "");
    }

    @Test
    public void testRenderHtmlContainsAttributionAndMetrics() throws JSONException {
        String json = "{\n" +
                "  \"predMPrazo\": {\n" +
                "    \"listaPredDiaMPrazo\": [\n" +
                "      {\n" +
                "        \"dataPredicion\": \"2026-08-18T00:00:00\",\n" +
                "        \"tMax\": 22,\n" +
                "        \"tMin\": 12,\n" +
                "        \"probIcoCeo1\": 5\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        Forecast forecast = parser.parseJson(testMunicipality, json);
        String html = parser.renderHtmlForecast(forecast);

        assertNotNull(html);
        assertTrue(html.contains("Santiago de Compostela"));
        assertTrue(html.contains("Calidade do Ar"));
        assertTrue(html.contains("Índice UV"));
        assertTrue(html.contains("Fonte dos datos: Xunta de Galicia – MeteoGalicia"));
        assertTrue(html.contains("CC BY-SA 4.0"));
    }
}
