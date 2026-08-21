package gal.rodrigosambade.tempogalicia;

import gal.rodrigosambade.tempogalicia.model.Municipality;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(JUnit4.class)
public class MunicipalityTest {

    @Test
    public void testDefaultMunicipalitiesList() {
        List<Municipality> list = Municipality.getDefaultMunicipalities();
        assertNotNull(list);
        assertFalse(list.isEmpty());
        assertEquals(313, list.size());

        Municipality arnoia = list.get(0);
        assertEquals("A Arnoia", arnoia.getName());
        assertEquals("32003", arnoia.getCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyNameThrowsException() {
        new Municipality("", "15030");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyCodeThrowsException() {
        new Municipality("Vigo", "");
    }
}
