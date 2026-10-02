# Errores encontrados en el cajero del compañero

## Resumen

Mi suite (MP-1 a MP-4) atrapó sola **3 de los 5** errores (quejas 1, 2 y 3). Las quejas 4 y 5 solo las atrapó MP-5.

| Queja | Prueba que la reproduce | Qué estaba mal en el código (la línea y por qué) | ¿Mi suite ya lo atrapaba? |
|:-----:|-------------------------|---------------------------------------------------|:-------------------------:|
| 1 | `sinSmsSiNoHaySaldo` | **Línea 50:** `avisar(...)` se llamaba antes de `cuenta.retirar(monto)`, así que el SMS salía aunque el retiro se rechazara. | ✅ **Sí** |
| 2 | `sospechosaNoMueveDinero` | **Línea 52:** `validarAntifraude(...)` estaba después de `cuenta.retirar(monto)` y del límite diario. | ✅ **Sí** |
| 3 | `seGuardaUnaVez` | **Líneas 53 y 54:** `repositorio.guardar(...)` estaba duplicada. | ✅ **Sí** |
| 4 | `antifraudeCaidoRechaza` | **Línea 67:** el `catch` asignaba `sospechosa = false`; debía ser `true`. | ❌ **No** |
| 5 | `limiteDiarioExacto` | **Línea 77:** la comparación era `>= 0`; debía ser `> 0`. | ❌ **No** |

## Detalle por queja

### Queja 1: SMS de un retiro rechazado por falta de saldo

- **Prueba:** `sinSmsSiNoHaySaldo`
- **Código:** línea 50, `avisar(...)` antes de `cuenta.retirar(monto)`.
- **Por qué está mal:** el SMS salía aunque el retiro se rechazara después con `SaldoInsuficienteException`. Rompe **C5** y el orden de **C6** (guardar y luego SMS).
- **¿Mi suite lo atrapaba?** ✅ **Sí.** `InteraccionesTest.saldoInsuficienteNoGuardaNiNotifica`: el notificador recibió una llamada desde `avisar`.

### Queja 2: operación sospechosa, pero el dinero sí salió

- **Prueba:** `sospechosaNoMueveDinero`
- **Código:** línea 52, `validarAntifraude(...)` después de `cuenta.retirar(monto)` y después del límite diario (línea 49).
- **Por qué está mal:** la operación se rechazaba con la cuenta ya descontada, y el SMS ya había salido. Rompe **C3**: el antifraude va justo después de buscar la cuenta.
- **¿Mi suite lo atrapaba?** ✅ **Sí**:
  - `InteraccionesTest.sospechosaNoGuardaNiNotifica`: se envió un SMS.
  - `StubsTest.sospechosa` y `AnotacionesTest.operacionSospechosaSeRechaza`: fallan con `NullPointerException`, porque el límite se consulta antes que el antifraude y `yaRetirado` es `null`.

### Queja 3: cada retiro llega dos veces a la base

- **Prueba:** `seGuardaUnaVez`
- **Código:** líneas 53 y 54, `repositorio.guardar(numeroCuenta, cuenta);` duplicada.
- **Por qué está mal:** cada retiro se guardaba dos veces. Rompe **C6**.
- **¿Mi suite lo atrapaba?** ✅ **Sí**:
  - `CajeroConDoblesAManoTest.retiroExitoso`: esperaba 1 y fueron 2.
  - `InteraccionesTest.retiroGuardaUnaVez`, `ordenDeLasLlamadas` y `smsFallaPeroRetiroSeMantiene`.

### Queja 4: con el antifraude caído, el cajero dejó retirar a todos

- **Prueba:** `antifraudeCaidoRechaza`
- **Código:** línea 67, en `validarAntifraude`, el `catch (ServicioNoDisponibleException e)` asignaba `sospechosa = false`.
- **Por qué está mal:** si el antifraude caía, el retiro procedía sin revisión. Debía ser `true`. Rompe **C3**.
- **¿Mi suite lo atrapaba?** ❌ **No.** Ninguna prueba de MP-1 a MP-4 hacía que el antifraude lanzara `ServicioNoDisponibleException`.

### Queja 5: no dejó retirar justo los $8,000.00 del día

- **Prueba:** `limiteDiarioExacto`
- **Código:** línea 77, en `validarLimiteDiario`, `compareTo(LIMITE_DIARIO) >= 0`.
- **Por qué está mal:** debía ser `> 0`. Con 7500.00 + 500.00 = 8000.00 rechazaba un retiro válido. Rompe **C4** («no puede pasar de»).
- **¿Mi suite lo atrapaba?** ❌ **No.** `StubsTest.limiteDiario` usaba 7600.00 + 500.00 = 8100.00, que se rechaza igual con `>=` y con `>`. Nunca probaba el valor justo en el límite.

## Pruebas que fallaron en `mp5-companero.txt` (8 de 19)

| Clase | Prueba |
|-------|--------|
| `CajeroConDoblesAManoTest` | `retiroExitoso` |
| `StubsTest` | `sospechosa` |
| `InteraccionesTest` | `retiroGuardaUnaVez` |
| `InteraccionesTest` | `ordenDeLasLlamadas` |
| `InteraccionesTest` | `smsFallaPeroRetiroSeMantiene` |
| `InteraccionesTest` | `saldoInsuficienteNoGuardaNiNotifica` |
| `InteraccionesTest` | `sospechosaNoGuardaNiNotifica` |
| `AnotacionesTest` | `operacionSospechosaSeRechaza` |