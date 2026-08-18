package gal.rodrigosambade.tempogalicia.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Representa un municipio galego e o seu código oficial de MeteoGalicia.
 */
public final class Municipality {

    private final String name;
    private final String code;

    public Municipality(String name, String code) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do municipio non pode estar baleiro");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("O código do municipio non pode estar baleiro");
        }
        this.name = name.trim();
        this.code = code.trim();
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Municipality that = (Municipality) o;
        return Objects.equals(name, that.name) && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, code);
    }

    @Override
    public String toString() {
        return name;
    }

    public static List<Municipality> getDefaultMunicipalities() {
        List<Municipality> list = new ArrayList<>();
        list.add(new Municipality("A Coruña", "15030"));
        list.add(new Municipality("Ferrol", "15036"));
        list.add(new Municipality("Lugo", "27028"));
        list.add(new Municipality("Ourense", "32054"));
        list.add(new Municipality("Pontevedra", "36038"));
        list.add(new Municipality("Santiago de Compostela", "15078"));
        list.add(new Municipality("Vigo", "36057"));
        return Collections.unmodifiableList(list);
    }
}
