package gal.rodrigosambade.tempogalicia;

import gal.rodrigosambade.tempogalicia.util.AstronomyUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class AstronomyUtilsTest {

    @Test
    public void testMoonPhaseCalculation() {
        String phase = AstronomyUtils.getMoonPhase("2026-08-18");
        assertNotNull(phase);
        assertFalse(phase.isEmpty());
    }

    @Test
    public void testAirQuality() {
        String airQuality = AstronomyUtils.getAirQuality(20);
        assertNotNull(airQuality);
        assertTrue(airQuality.contains("Boa") || airQuality.contains("Aceptable"));
    }

    @Test
    public void testUvIndex() {
        int uv = AstronomyUtils.getUvIndex(25, 10);
        assertTrue(uv >= 1 && uv <= 11);
    }
}
