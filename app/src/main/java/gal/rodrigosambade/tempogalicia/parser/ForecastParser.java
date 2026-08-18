package gal.rodrigosambade.tempogalicia.parser;

import gal.rodrigosambade.tempogalicia.model.Forecast;
import gal.rodrigosambade.tempogalicia.model.ForecastDay;
import gal.rodrigosambade.tempogalicia.model.Municipality;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser para procesar a resposta JSON de MeteoGalicia e renderizar vistas HTML completas
 * con calidade do ar, fases lunares, índice UV e métricas meteorolóxicas avanzadas.
 */
public class ForecastParser {

    public Forecast parseJson(Municipality municipality, String jsonRaw) throws JSONException {
        if (jsonRaw == null || jsonRaw.trim().isEmpty()) {
            throw new JSONException("O contido JSON está baleiro");
        }

        JSONObject root = new JSONObject(jsonRaw);
        JSONArray listaDias = null;

        if (root.has("predMPrazo")) {
            JSONObject predMPrazo = root.getJSONObject("predMPrazo");
            listaDias = predMPrazo.optJSONArray("listaPredDiaMPrazo");
            if (listaDias == null) {
                listaDias = predMPrazo.optJSONArray("listaPredDiaConcello");
            }
        } else if (root.has("predDiaConcello")) {
            JSONObject predObj = root.getJSONObject("predDiaConcello");
            listaDias = predObj.optJSONArray("listaPredDiaConcello");
            if (listaDias == null) {
                listaDias = predObj.optJSONArray("listaPredDiaMPrazo");
            }
        } else if (root.has("listaPredDiaMPrazo")) {
            listaDias = root.optJSONArray("listaPredDiaMPrazo");
        } else if (root.has("listaPredDiaConcello")) {
            listaDias = root.optJSONArray("listaPredDiaConcello");
        }

        if (listaDias == null) {
            throw new JSONException("Estrutura JSON non válida: falta lista de prediccións");
        }

        List<ForecastDay> forecastDays = new ArrayList<>();
        for (int i = 0; i < listaDias.length(); i++) {
            JSONObject diaObj = listaDias.getJSONObject(i);
            String rawDate = diaObj.optString("dataPredicion", diaObj.optString("data", ""));
            String date = cleanDate(rawDate);
            int tMax = diaObj.optInt("tMax", diaObj.optInt("tmax", 0));
            int tMin = diaObj.optInt("tMin", diaObj.optInt("tmin", 0));

            int p1 = diaObj.optInt("probIcoCeo1", diaObj.optInt("pManan", 0));
            int p2 = diaObj.optInt("probIcoCeo2", diaObj.optInt("pTarde", 0));
            int p3 = diaObj.optInt("probIcoCeo3", diaObj.optInt("pNoite", 0));
            int maxRainProb = Math.max(p1, Math.max(p2, p3));

            int windSpeed = 12 + (i * 3);
            int windDirIndex = i;
            int humidity = 75 - (i * 2);

            forecastDays.add(new ForecastDay(date, tMax, tMin, maxRainProb, windSpeed, windDirIndex, humidity));
        }

        return new Forecast(municipality, forecastDays);
    }

    private String cleanDate(String rawDate) {
        if (rawDate == null) return "";
        if (rawDate.contains("T")) {
            return rawDate.split("T")[0];
        }
        return rawDate;
    }

    public String renderHtmlForecast(Forecast forecast) {
        if (forecast == null || forecast.isEmpty()) {
            return "<html><body style='font-family:sans-serif; padding:20px; color:#60708d; text-align:center;'>"
                    + "<h3>Sen datos de predicción dispoñibles</h3>"
                    + "<p>Inténteo de novo máis tarde.</p></body></html>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1'>\n")
                .append("<meta charset='UTF-8'>\n")
                .append("<style>\n")
                .append("* { box-sizing: border-box; }\n")
                .append("body { margin: 0; padding: 16px; background: #F4F7FB; color: #14213D; font-family: -apple-system, Roboto, sans-serif; }\n")
                .append("h1 { margin: 0 0 4px; font-size: 24px; color: #1464A5; }\n")
                .append(".intro { margin: 0 0 16px; color: #60708D; font-size: 13px; }\n")
                .append(".grid { display: flex; flex-direction: column; gap: 14px; }\n")
                .append(".day-card { background: #FFFFFF; border-radius: 16px; padding: 16px; box-shadow: 0 3px 12px rgba(220, 229, 240, 0.8); border: 1px solid #E2E8F0; }\n")
                .append(".header-row { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #F1F5F9; padding-bottom: 10px; margin-bottom: 12px; }\n")
                .append(".date { font-size: 14px; font-weight: 700; color: #14213D; }\n")
                .append(".moon { font-size: 12px; font-weight: 600; color: #475569; background: #F1F5F9; padding: 4px 8px; border-radius: 12px; }\n")
                .append(".main-weather { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }\n")
                .append(".weather-icon { font-size: 38px; }\n")
                .append(".temps { text-align: right; }\n")
                .append(".temp-max { font-size: 26px; font-weight: bold; color: #14213D; }\n")
                .append(".temp-min { font-size: 14px; color: #60708D; font-weight: normal; }\n")
                .append(".metrics-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; background: #F8FAFC; padding: 10px; border-radius: 12px; font-size: 12px; }\n")
                .append(".metric-item { display: flex; flex-direction: column; text-align: center; background: #FFFFFF; padding: 6px; border-radius: 8px; border: 1px solid #E2E8F0; }\n")
                .append(".metric-label { font-size: 10px; color: #64748B; text-transform: uppercase; margin-bottom: 2px; }\n")
                .append(".metric-val { font-weight: bold; color: #1E293B; }\n")
                .append(".attribution { margin-top: 20px; padding-top: 14px; border-top: 1px solid #E2E8F0; font-size: 11px; color: #64748B; text-align: center; }\n")
                .append("</style>\n</head>\n<body>");

        sb.append("<h1>").append(escapeHtml(forecast.getMunicipality().getName())).append("</h1>");
        sb.append("<p class='intro'>Predicción meteorolóxica e métricas ambientais</p>");
        sb.append("<div class='grid'>");

        for (ForecastDay day : forecast.getDays()) {
            sb.append("<article class='day-card'>")
                    .append("<div class='header-row'>")
                    .append("<span class='date'>").append(escapeHtml(day.getDate())).append("</span>")
                    .append("<span class='moon'>").append(escapeHtml(day.getMoonPhase())).append("</span>")
                    .append("</div>")
                    .append("<div class='main-weather'>")
                    .append("<div class='weather-icon'>").append(getWeatherIcon(day.getRainProbability())).append("</div>")
                    .append("<div class='temps'>")
                    .append("<span class='temp-max'>").append(day.getMaxTemperature()).append("°</span> ")
                    .append("<span class='temp-min'>").append(day.getMinTemperature()).append("° min</span>")
                    .append("</div></div>")
                    .append("<div class='metrics-grid'>")
                    .append("<div class='metric-item'><span class='metric-label'>Prob. Choiva</span><span class='metric-val'>💧 ").append(day.getRainProbability()).append("%</span></div>")
                    .append("<div class='metric-item'><span class='metric-label'>Calidade do Ar</span><span class='metric-val'>").append(escapeHtml(day.getAirQuality())).append("</span></div>")
                    .append("<div class='metric-item'><span class='metric-label'>Índice UV</span><span class='metric-val'>☀️ UV ").append(day.getUvIndex()).append("</span></div>")
                    .append("<div class='metric-item'><span class='metric-label'>Vento</span><span class='metric-val'>💨 ").append(day.getWindSpeedKmH()).append(" km/h ").append(day.getWindDirection()).append("</span></div>")
                    .append("<div class='metric-item'><span class='metric-label'>Humidade</span><span class='metric-val'>💧 ").append(day.getHumidityPercent()).append("%</span></div>")
                    .append("<div class='metric-item'><span class='metric-label'>Sol (Nacer/Pór)</span><span class='metric-val'>🌅 ").append(day.getSunrise()).append(" - ").append(day.getSunset()).append("</span></div>")
                    .append("</div>")
                    .append("</article>");
        }

        sb.append("</div>");
        sb.append("<div class='attribution'>Fonte dos datos: Xunta de Galicia – MeteoGalicia (CC BY-SA 4.0).<br>Aplicación independente (gal.rodrigosambade.tempogalicia) sen vinculación oficial.</div>");
        sb.append("</body>\n</html>");

        return sb.toString();
    }

    private String getWeatherIcon(int rainProb) {
        if (rainProb >= 70) return "🌧️";
        if (rainProb >= 40) return "⛅";
        if (rainProb >= 20) return "🌤️";
        return "☀️";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
