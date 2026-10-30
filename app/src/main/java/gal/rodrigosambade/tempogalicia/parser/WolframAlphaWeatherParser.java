package gal.rodrigosambade.tempogalicia.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import gal.rodrigosambade.tempogalicia.util.WeatherIcons;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders official weather pods as safe, Spanish-labelled HTML.
 * Text not covered by the meteorological glossary remains visibly marked as original.
 * Does not invent values or translate charts without plaintext.
 */
public final class WolframAlphaWeatherParser {
    private static final Map<String, String> GLOSSARY = new LinkedHashMap<>();
    static {
        String[][] terms = {
                {"partly cloudy","parcialmente nuboso"}, {"mostly cloudy","mayormente nuboso"},
                {"mostly sunny","mayormente soleado"}, {"partly sunny","parcialmente soleado"},
                {"scattered showers","chubascos dispersos"}, {"light rain","lluvia ligera"},
                {"heavy rain","lluvia intensa"}, {"light snow","nieve ligera"},
                {"thunderstorms","tormentas"}, {"thunderstorm","tormenta"},
                {"precipitation","precipitación"}, {"temperature","temperatura"},
                {"feels like","sensación térmica"}, {"wind speed","velocidad del viento"},
                {"wind direction","dirección del viento"}, {"relative humidity","humedad relativa"},
                {"humidity","humedad"}, {"dew point","punto de rocío"},
                {"cloud cover","nubosidad"}, {"visibility","visibilidad"},
                {"pressure","presión"}, {"high","máxima"}, {"low","mínima"},
                {"maximum","máxima"}, {"minimum","mínima"}, {"current","actual"},
                {"conditions","condiciones"}, {"forecast","previsión"},
                {"weather","tiempo"}, {"observed","observado"},
                {"recorded","registrado"}, {"updated","actualizado"},
                {"average","media"}, {"chance of rain","probabilidad de lluvia"},
                {"probability of rain","probabilidad de lluvia"},
                {"rain","lluvia"}, {"snow","nieve"}, {"clear","despejado"},
                {"sunny","soleado"}, {"cloudy","nuboso"}, {"overcast","cubierto"},
                {"fog","niebla"}, {"mist","neblina"}, {"windy","ventoso"},
                {"showers","chubascos"}, {"drizzle","llovizna"},
                {"northwest","noroeste"}, {"northeast","nordeste"},
                {"southwest","suroeste"}, {"southeast","sureste"},
                {"north","norte"}, {"south","sur"}, {"east","este"}, {"west","oeste"},
                {"today","hoy"}, {"tomorrow","mañana"}, {"yesterday","ayer"},
                {"monday","lunes"}, {"tuesday","martes"}, {"wednesday","miércoles"},
                {"thursday","jueves"}, {"friday","viernes"}, {"saturday","sábado"},
                {"sunday","domingo"}, {"january","enero"}, {"february","febrero"},
                {"march","marzo"}, {"april","abril"}, {"may","mayo"}, {"june","junio"},
                {"july","julio"}, {"august","agosto"}, {"september","septiembre"},
                {"october","octubre"}, {"november","noviembre"}, {"december","diciembre"},
                {"degrees celsius","grados Celsius"}, {"degrees fahrenheit","grados Fahrenheit"},
                {"kilometers per hour","kilómetros por hora"}, {"miles per hour","millas por hora"},
                {"km/h","km/h"}, {"mph","mph"}, {"inches","pulgadas"},
                {"hour","hora"}, {"hours","horas"}, {"day","día"}, {"days","días"},
                {"at","a las"}, {"from","desde"}, {"to","hasta"},
                {"of","de"}, {"for","para"}, {"and","y"}
        };
        for (String[] pair : terms) GLOSSARY.put(pair[0], pair[1]);
    }

    public String render(String json, String municipality) throws JSONException {
        JSONObject root = new JSONObject(json).optJSONObject("queryresult");
        if (root == null) throw new JSONException("Respuesta de Wolfram|Alpha inválida");
        if (root.optBoolean("error")) throw new JSONException("Wolfram|Alpha devolvió un error de consulta");
        if (!root.optBoolean("success"))
            throw new JSONException("Wolfram|Alpha no ha identificado datos para este concello");

        StringBuilder html = new StringBuilder(4096);
        html.append("<!doctype html><html lang='es'><head><meta charset='utf-8'>")
                .append("<meta name='viewport' content='width=device-width,initial-scale=1'>")
                .append("<style>body{font-family:sans-serif;background:#f4f8fc;color:#213246;padding:16px;line-height:1.5}")
                .append("h1{font-size:21px}h2{font-size:17px;margin:0 0 8px;color:#145993}")
                .append("section{padding:15px;margin:12px 0;background:white;border-radius:12px}")
                .append("p{white-space:pre-wrap;margin:0}small{color:#576979}.weather-symbol{width:76px;height:76px;display:block;margin:3px 0 8px}</style></head><body>")
                .append("<h1>El tiempo en ").append(escapeHtml(municipality)).append("</h1>")
                .append("<small>Fuente: Wolfram|Alpha · Consulta: ")
                .append(escapeHtml("weather " + municipality)).append("</small>");

        Object pods = root.opt("pods");
        int added = 0;
        for (int i = 0; i < count(pods) && added < 12; i++) {
            JSONObject pod = item(pods, i);
            if (pod == null) continue;
            String id = pod.optString("id", "");
            if ("Input".equalsIgnoreCase(id) || id.startsWith("Assumptions")) continue;
            Object subpods = pod.opt("subpods");
            StringBuilder contents = new StringBuilder();
            for (int j = 0; j < count(subpods); j++) {
                JSONObject part = item(subpods, j);
                if (part == null) continue;
                String data = part.optString("plaintext", "").trim();
                if (!data.isEmpty()) {
                    if (contents.length() != 0) contents.append("\n\n");
                    contents.append(data);
                }
            }
            if (contents.length() == 0) continue;
            String title = translatedHeading(id, pod.optString("title", ""));
            html.append("<section><h2>").append(escapeHtml(title)).append("</h2>")
                    .append(WeatherIcons.htmlIcon(WeatherIcons.forConditions(contents.toString())))
                    .append("<p>").append(escapeHtml(translateWeatherTerms(contents.toString())))
                    .append("</p></section>");
            added++;
        }
        if (added == 0)
            html.append("<section>Wolfram|Alpha no ha proporcionado datos meteorológicos textuales para este concello.</section>");
        html.append("<small>Traducción orientativa de términos meteorológicos; algunos nombres, ")
                .append("valores y expresiones pueden permanecer en el idioma original. ")
                .append("No se han estimado datos ausentes.</small></body></html>");
        return html.toString();
    }

    private static int count(Object json) {
        return json instanceof JSONArray ? ((JSONArray) json).length()
                : json instanceof JSONObject ? 1 : 0;
    }

    private static JSONObject item(Object json, int position) {
        return json instanceof JSONArray ? ((JSONArray) json).optJSONObject(position)
                : position == 0 && json instanceof JSONObject ? (JSONObject) json : null;
    }

    static String translatedHeading(String id, String original) {
        if (id.startsWith("InstantaneousWeather")) return "Últimos datos meteorológicos registrados";
        if (id.startsWith("WeatherForecast")) return "Previsión meteorológica";
        if (id.startsWith("WeatherCharts")) return "Evolución del tiempo";
        if (id.startsWith("HistoricalTemperature")) return "Temperaturas históricas";
        if (id.startsWith("WeatherStation")) return "Información de la estación meteorológica";
        if (id.startsWith("LocalTemperature")) return "Comparación de temperaturas cercanas";
        return translateWeatherTerms(original);
    }

    public static String translateWeatherTerms(String original) {
        if (original == null) return "";
        String result = original;
        for (Map.Entry<String, String> term : GLOSSARY.entrySet()) {
            Pattern pattern = Pattern.compile("(?i)(?<![\\p{L}])" + Pattern.quote(term.getKey()) + "(?![\\p{L}])");
            result = pattern.matcher(result).replaceAll(Matcher.quoteReplacement(term.getValue()));
        }
        return result;
    }

    public static String escapeHtml(String original) {
        if (original == null) return "";
        return original.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
