# Lista de Comprobación para la Publicación en Google Play Store

Este documento contiene los pasos manuales que debe seguir el desarrollador antes de publicar **Tempo Galicia – Previsión** en Google Play Console.

---

## 1. Verificación Previa y Nombre
- [ ] Verificar disponibilidad del nombre comercial **Tempo Galicia – Previsión** en Google Play Store.
- [ ] Confirmar que no se utilicen marcas registradas ni nombres confusos con aplicaciones oficiales en el título ni en los textos promocionales.

## 2. Generación del Keystore y Firma de Release
> [!CAUTION]
> NUNCA subas archivos `.jks`, `.keystore` o contraseñas al repositorio Git.

- [ ] Generar un Keystore de producción de forma local fuera del repositorio:
  ```bash
  keytool -genkey -v -keystore release.jks -alias tempogalicia -keyalg RSA -keysize 2048 -validity 10000
  ```
- [ ] Configurar las credenciales en tu `gradle.properties` local (en `~/.gradle/gradle.properties`):
  ```properties
  MYAPP_RELEASE_STORE_FILE=/ruta/segura/release.jks
  MYAPP_RELEASE_STORE_PASSWORD=****
  MYAPP_RELEASE_KEY_ALIAS=tempogalicia
  MYAPP_RELEASE_KEY_PASSWORD=****
  ```

## 3. Compilación del Android App Bundle (AAB)
- [ ] Ejecutar la compilación del paquete de distribución:
  ```bash
  ./gradlew bundleRelease
  ```
- [ ] Verificar que el archivo `.aab` generado en `app/build/outputs/bundle/release/app-release.aab` compile correctamente.

## 4. Configuración en Google Play Console
- [ ] Crear la aplicación en la Play Console con el Package ID `com.rodrigosambade.tempogalicia`.
- [ ] Completar la **Ficha Comercial** utilizando el contenido preparado en `docs/GOOGLE_PLAY_LISTING.md`.
- [ ] Subir capturas de pantalla (Screenshots) de la aplicación en dispositivos teléfono y tablet.
- [ ] Subir el Gráfico de Funciones (Feature Graphic 1024x500 px).
- [ ] Rellenar el cuestionario de **Data Safety** indicando que la aplicación NO recopila datos personales.
- [ ] Completar la evaluación de **Clasificación de Contenido** (PEGI 3 / Everyone).
- [ ] Declarar la URL de la **Política de Privacidad** (Privacy Policy).

## 5. Publicación
- [ ] Subir el paquete `app-release.aab` a la pista de pruebas internas/cerradas o producción.
- [ ] Revisar que la verificación de firma por Play App Signing esté activada.
- [ ] Enviar a revisión.
