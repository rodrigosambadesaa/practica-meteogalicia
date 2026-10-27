# Wolfram|Alpha: segundo proveedor del tiempo

La pantalla principal permite elegir **concello** (los 313 del catálogo de MeteoGalicia) y **fuente**: MeteoGalicia o Wolfram|Alpha.

## Configuración

1. Consigue un AppID válido y autorizado para **Full Results API** en https://developer.wolframalpha.com/portal/myapps/.
2. Elige **Wolfram|Alpha** en el selector de fuente e introduce el AppID cuando lo solicite.
3. Selecciona un concello; por ejemplo, Santiago de Compostela. La petición oficial usa `weather Santiago de Compostela`, con `location=Galicia, Spain`, `units=metric`, `format=plaintext` y `output=json`.
4. Si el AppID deja de funcionar, vuelve a seleccionar Wolfram|Alpha para acceder a la configuración (o borra el almacenamiento de la aplicación).

El AppID **no está incluido en el repositorio**, ni en el APK precompilado, ni en GitHub Actions; se guarda en preferencias privadas del dispositivo (con copia de seguridad Android desactivada). **No es un almacén criptográficamente seguro**: en una distribución pública es preferible un servicio intermediario que proteja las credenciales y aplique cuotas. El identificador viaja a Wolfram por HTTPS. La disponibilidad, restricciones y coste dependen del plan de la API contratado; no se incluye una cuenta de servicio.

## Traducción y límites

La consulta y la respuesta original se hacen en inglés. El parser interpreta los `pods` y `subpods` textuales, traduce etiquetas y terminología meteorológica frecuente al español, y escapa el HTML antes de mostrarlo sin JavaScript.

**Importante:** esta es una **traducción meteorológica parcial**, no un motor general de traducción automática. Algunas expresiones, nombres propios o textos ajenos al glosario quedan en su idioma original. Los gráficos sin contenido `plaintext` no se reproducen. El programa no inventa temperaturas ni extrapola predicciones en ausencia de datos.

Se mantiene íntegra la clase `ConnectivityAndInternetAccess`; cada solicitud real prueba primero su backend y solo ejecuta un diagnóstico general si hay un fallo de transporte. El widget y las notificaciones continúan utilizando exclusivamente MeteoGalicia, para no mezclar fuentes ni publicar datos de distinta estructura.

## Pruebas

Ejecutar `./gradlew testDebugUnitTest assembleDebug lintDebug`, o consultar Android CI en la PR. Las pruebas `WolframAlphaWeatherParserTest` verifican URL y concello, traducción conocida, escaping HTML, respuesta JSON con pods simples y errores de Wolfram|Alpha. La consulta real no se puede probar con datos de producción sin AppID válido.
