package gal.rodrigosambade.tempogalicia.model;

import gal.rodrigosambade.tempogalicia.util.AstronomyUtils;

import java.util.Objects;

/**
 * Representa os datos meteorolóxicos e astronómicos dun día concreto.
 */
public final class ForecastDay {

    private final String date;
    private final int maxTemperature;
    private final int minTemperature;
    private final int rainProbability;
    private final String moonPhase;
    private final String airQuality;
    private final int uvIndex;
    private final int windSpeedKmH;
    private final String windDirection;
    private final int humidityPercent;
    private final String sunrise;
    private final String sunset;

    public ForecastDay(String date, int maxTemperature, int minTemperature, int rainProbability,
                       int windSpeedKmH, int windDirIndex, int humidityPercent) {
        this.date = date != null ? date : "";
        this.maxTemperature = maxTemperature;
        this.minTemperature = minTemperature;
        this.rainProbability = Math.max(0, Math.min(100, rainProbability));
        this.moonPhase = AstronomyUtils.getMoonPhase(this.date);
        this.airQuality = AstronomyUtils.getAirQuality(this.rainProbability);
        this.uvIndex = AstronomyUtils.getUvIndex(maxTemperature, rainProbability);
        this.windSpeedKmH = Math.max(5, windSpeedKmH);
        this.windDirection = AstronomyUtils.getWindDirection(windDirIndex);
        this.humidityPercent = Math.max(40, Math.min(99, humidityPercent));
        this.sunrise = AstronomyUtils.getSunrise(this.date);
        this.sunset = AstronomyUtils.getSunset(this.date);
    }

    public String getDate() { return date; }
    public int getMaxTemperature() { return maxTemperature; }
    public int getMinTemperature() { return minTemperature; }
    public int getRainProbability() { return rainProbability; }
    public String getMoonPhase() { return moonPhase; }
    public String getAirQuality() { return airQuality; }
    public int getUvIndex() { return uvIndex; }
    public int getWindSpeedKmH() { return windSpeedKmH; }
    public String getWindDirection() { return windDirection; }
    public int getHumidityPercent() { return humidityPercent; }
    public String getSunrise() { return sunrise; }
    public String getSunset() { return sunset; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForecastDay day = (ForecastDay) o;
        return maxTemperature == day.maxTemperature &&
                minTemperature == day.minTemperature &&
                rainProbability == day.rainProbability &&
                Objects.equals(date, day.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, maxTemperature, minTemperature, rainProbability);
    }

    @Override
    public String toString() {
        return "ForecastDay{" +
                "date='" + date + '\'' +
                ", maxTemp=" + maxTemperature +
                ", minTemp=" + minTemperature +
                ", rainProb=" + rainProbability +
                ", moonPhase='" + moonPhase + '\'' +
                '}';
    }
}
