package gal.rodrigosambade.tempogalicia.notification;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(JUnit4.class)
public class WeatherNotificationHelperTest {

    @Test
    public void testNotificationChannelConstants() {
        assertNotNull(WeatherNotificationHelper.CHANNEL_ID_WEATHER);
        assertNotNull(WeatherNotificationHelper.CHANNEL_ID_NETWORK);
        assertEquals("tempo_galicia_weather_channel", WeatherNotificationHelper.CHANNEL_ID_WEATHER);
        assertEquals("tempo_galicia_network_channel", WeatherNotificationHelper.CHANNEL_ID_NETWORK);
    }
}
