package gal.rodrigosambade.tempogalicia.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Contedor da predicción meteorolóxica completa dun municipio.
 */
public final class Forecast {

    private final Municipality municipality;
    private final List<ForecastDay> days;

    public Forecast(Municipality municipality, List<ForecastDay> days) {
        this.municipality = Objects.requireNonNull(municipality, "O municipio é obrigatorio");
        this.days = days != null ? Collections.unmodifiableList(new ArrayList<>(days)) : Collections.emptyList();
    }

    public Municipality getMunicipality() {
        return municipality;
    }

    public List<ForecastDay> getDays() {
        return days;
    }

    public boolean isEmpty() {
        return days.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Forecast forecast = (Forecast) o;
        return Objects.equals(municipality, forecast.municipality) &&
                Objects.equals(days, forecast.days);
    }

    @Override
    public int hashCode() {
        return Objects.hash(municipality, days);
    }

    @Override
    public String toString() {
        return "Forecast{" +
                "municipality=" + municipality +
                ", daysCount=" + days.size() +
                '}';
    }
}
