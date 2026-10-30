# Iconos meteorológicos compartidos por las dos fuentes

Se han integrado **Meteocons**, del repositorio https://github.com/rodrigosambadesaa/weather-icons (autor original: Bas Milius), en los resultados de MeteoGalicia y Wolfram|Alpha.

## Funcionamiento
- Los SVG estáticos de la variante `production/fill/svg-static` están incorporados en `app/src/main/assets/meteocons`. No se descargan de internet ni se hace ninguna solicitud adicional para los iconos.
- El HTML de las tarjetas de MeteoGalicia incorpora marcadores `weather-icon://`, usando el porcentaje de probabilidad de lluvia del pronóstico para seleccionar un **icono orientativo**, no una observación del cielo.
- Los apartados textuales de Wolfram|Alpha muestran el icono correspondiente cuando se reconoce una condición meteorológica (lluvia, nieve, niebla, tormenta, nubes, sol, viento). Si un apartado de **condiciones actuales o previsión** no contiene un estado meteorológico reconocible, se muestra el icono **«no disponible»** y nunca se inventa una condición. Se consideran también los títulos de subapartados y los textos alternativos disponibles.
- Los marcadores se sustituyen por **SVG insertados directamente en el documento HTML** antes de cargarlo en la WebView, en lugar de cargarlos como imágenes `data:`. Se renombran los identificadores y referencias internas de cada SVG para evitar colisiones entre varios iconos (gradientes y símbolos). No se requiere acceso a archivos locales desde WebView, JavaScript ni peticiones gráficas externas.
- El estado de conectividad, las peticiones a los proveedores, el selector de concello/fuente, las notificaciones y el widget se mantienen tal como estaban.

## Licencia
Meteocons se distribuye bajo **MIT**, © 2020–2022 Bas Milius. Se incluye `app/src/main/assets/meteocons/LICENSE` y el reconocimiento en `THIRD_PARTY_NOTICES.md`. Véase también el repositorio original https://github.com/basmilius/weather-icons.

## Pruebas
`WeatherIconsTest` verifica clasificación, HTML de ambas fuentes, icono «no disponible» y protección frente a rutas no válidas. `WeatherIconAssetsTest` comprueba que los SVG locales están realmente insertados en el documento con identificadores únicos y que existe una representación visible de reserva si falta un archivo. Además se ejecuta la validación existente con `./gradlew testDebugUnitTest assembleDebug lintDebug`.

No se han modificado `ConnectivityAndInternetAccess.java` ni `NetworkValidationManager.java`.
