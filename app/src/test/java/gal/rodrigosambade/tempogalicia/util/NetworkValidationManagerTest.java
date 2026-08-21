package gal.rodrigosambade.tempogalicia.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class NetworkValidationManagerTest {

    @Test
    public void testPrimaryHostsConfiguration() {
        List<String> hosts = NetworkValidationManager.METEOGALICIA_PRIMARY_HOSTS;
        assertNotNull(hosts);
        assertEquals(3, hosts.size());
        assertTrue(hosts.contains("https://servizos.meteogalicia.gal/"));
        assertTrue(hosts.contains("https://www.meteogalicia.gal/"));
        assertTrue(hosts.contains("https://www.xunta.gal/"));
    }

    @Test
    public void testDefaultDnsResolvers() {
        List<String> resolvers = NetworkValidationManager.DEFAULT_DNS_RESOLVERS;
        assertNotNull(resolvers);
        assertFalse(resolvers.isEmpty());
        assertTrue(resolvers.contains("1.1.1.1"));
        assertTrue(resolvers.contains("8.8.8.8"));
    }
}
