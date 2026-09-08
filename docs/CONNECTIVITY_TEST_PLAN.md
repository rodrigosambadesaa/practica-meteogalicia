# Plan de pruebas de conectividad

La política para iniciar una operación remota es:

```java
ConnectivityAndInternetAccess.isConnected(context)
        && ConnectivityAndInternetAccess.hasPhysicalNetwork(context)
```

`hasPhysicalNetwork()` solo acepta Wi-Fi, datos móviles o Ethernet utilizables; una VPN aislada no es suficiente.

## Matriz manual

1. Wi-Fi activo, Internet funcional, VPN desactivada: la app permite cargar la previsión.
2. Wi-Fi activo, Internet funcional, AdGuard/VPN activa: la app permite cargar la previsión.
3. AdGuard/VPN activa, Wi-Fi y datos móviles desactivados, sin Ethernet: la app muestra offline y no inicia la petición remota.
4. Desde el caso anterior, activar Wi-Fi o datos: el `NetworkObserver` actualiza la UI y las nuevas peticiones vuelven a permitirse sin reiniciar la app.
5. VPN desactivada y sin redes: la app muestra offline.
6. Portal cautivo: `hasPhysicalNetwork()` permanece independiente de `VALIDATED`; `CAPTIVE_PORTAL` solo se muestra cuando Android lo detecta positivamente.

El caso VPN-only requiere un dispositivo o emulador con una `VpnService` real; los tests unitarios cubren la decisión booleana, no simulan el estado del sistema Android.
