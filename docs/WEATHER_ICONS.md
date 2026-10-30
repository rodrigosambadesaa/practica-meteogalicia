# Iconos meteorológicos compartidos por las dos fuentes

Se han integrado **Meteocons**, del repositorio https://github.com/rodrigosambadesaa/weather-icons (autor original: Bas Milius), en los resultados de MeteoGalicia y Wolfram|Alpha.

## Funcionamiento
- Los SVG estáticos de la variante `production/fill/svg-static` están incorporados en `app/src/main/assets/meteocons`. No se descargan de internet ni se hace ninguna solicitud adicional para los iconos.
- El HTML de las tarjetas de MeteoGalicia incorpora marcadores `weather-icon://`, usando el porcentaje de probabilidad de lluvia del pronóstico para seleccionar un **icono orientativo**, no una observación del cielo.
- Los apartados textuales de Wolfram|Alpha reciben un icono **solo cuando hay una condición meteorológica reconocible** (lluvia, nieve, niebla, tormenta, nubes, sol, viento). La ausencia de información no se sustituye por una condición inventada.
- Los marcadores se reemplazan por SVG incrustados en `data:image/svg+xml;base64` antes de cargar el HTML en la WebView. Así no es necesario permitir accesos a archivos locales ni habilitar JavaScript.
- El estado de conectividad, las peticiones a los proveedores, el selector de concello/fuente, las notificaciones y el widget se mantienen tal como estaban.

## Licencia
Meteocons se distribuye bajo **MIT**, © 2020–2022 Bas Milius. Se incluye `app/src/main/assets/meteocons/LICENSE` y el reconocimiento en `THIRD_PARTY_NOTICES.md`. Véase también el repositorio original https://github.com/basmilius/weather-icons.

## Pruebas
`WeatherIconsTest` verifica clasificación, HTML de ambas fuentes, protección frente a rutas no válidas y existencia de los SVG locales. Además se ejecuta la validación existente con `./gradlew testDebugUnitTest assembleDebug lintDebug`.

No se han modificado `ConnectivityAndInternetAccess.java` ni `NetworkValidationManager.java`.
