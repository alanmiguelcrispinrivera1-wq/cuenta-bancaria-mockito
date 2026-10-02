# Cajero automático — pruebas con JUnit 5 y Mockito

**Autor:** Alan Miguel Crispin Rivera

## Cómo correr las pruebas

    ./probar.sh final

## Qué prueba cada clase

| Clase de prueba | Qué prueba | Qué dobles usa | Cuántas pruebas |
|---|---|---|---|
| CajeroConDoblesAManoTest | Un retiro exitoso (baja el saldo, guarda la cuenta y manda un SMS) y que una cuenta inexistente se rechaza sin mandar SMS | fakes hechos a mano y una lambda | 2 |
| StubsTest | Lo que el cajero hace con las respuestas que se configuran: consultar saldo, repositorio caído, cuenta inexistente, mock sin configurar, retiro normal, operación sospechosa y límite diario | mocks de Mockito usados como stubs (`when`, `thenReturn`, `thenThrow`) y matchers `eq` / `any` | 7 |
| InteraccionesTest | Qué llamadas ocurrieron: guardar una sola vez, texto del SMS, operación sospechosa, cuenta inexistente, orden (antifraude, guardar, SMS), SMS que falla y saldo insuficiente | mocks de Mockito con `verify`, `never`, `verifyNoInteractions`, `ArgumentCaptor`, `InOrder` y `doThrow` | 7 |
| AnotacionesTest | Texto del SMS, cuenta inexistente sin antifraude ni SMS, y operación sospechosa, con stubs estrictos dentro de cada prueba | `@Mock` y `@Captor` con `MockitoExtension` | 3 |
| QuejasCajeroTest | Las 5 quejas sobre el cajero del compañero, cada una reproducida con una prueba (SMS sin saldo, sospechosa que mueve dinero, doble guardado, antifraude caído y límite exacto de $8,000.00) | `mock()` sin extensión, con los stubs comunes en el `@BeforeEach` | 5 |
| RelojTest | Que el límite diario usa la fecha del reloj del cajero en la zona de CDMX: a las 21:30 de CDMX sigue siendo el mismo día aunque en UTC ya sea el siguiente, y al día siguiente el límite empieza de cero | `Clock.fixed` (un reloj real fijo, no un mock) y un mock del repositorio | 3 |

## Boleto de salida

1. **¿Qué diferencia hay entre un stub y un mock? Da un ejemplo de cada uno de hoy.**

   Un stub sirve para *darle respuestas* al código que se prueba; un mock sirve para *comprobar qué llamadas recibió*. Con Mockito ambos se crean con `mock()`, y lo que cambia es el papel que juegan en la prueba.

   - **Stub:** `when(repositorio.buscar("001")).thenReturn(Optional.of(cuenta));` hace que el repositorio devuelva la cuenta que yo decido (StubsTest).
   - **Mock:** `verify(repositorio, times(1)).guardar("001", cuenta);` comprueba que el cajero guardó la cuenta exactamente una vez (QuejasCajeroTest e InteraccionesTest).

2. **¿Por qué la prueba inútil de la MP-6 salía en verde con el cajero roto?**

    Porque mockeaba justo lo que debía probar. `PruebaInutilTest` usa un mock de `CuentaBancaria` y le dice `when(cuentaFalsa.getSaldo()).thenReturn(new BigDecimal("1500.00"))`. Al borrar `cuenta.retirar(monto)` en el cajero, el cajero ya no descuenta nada, pero la cuenta falsa sigue respondiendo 1500.00 cuando `consultarSaldo` le pregunta. El `assertEquals` solo comprobaba que el mock devolvía el valor que yo mismo le había dicho, no que el cajero hiciera bien el retiro. Mientras tanto, varias de mis otras pruebas, que usan la cuenta real, sí se ponían en rojo (la guía contó 9). Una prueba que no puede fallar es peor que ninguna, porque da confianza falsa.

3. **¿Qué NO mockearías en un proyecto de Spring Boot como el de la Semana 3, y por qué?**

   - **La clase que estoy probando** (en este proyecto, `CajeroAutomatico`; en Spring Boot, el `@Service` bajo prueba): si la mockeo, solo pruebo a Mockito.
   - **Los objetos de valor y lo mío que es simple**, como `CuentaBancaria`, `Movimiento`, `BigDecimal` o `LocalDate`: tienen lógica propia, son baratos de crear y mockearlos fue justo el error de la prueba inútil.
   - **El reloj:** no lo mockeo con Mockito; uso `Clock.fixed(...)`, un doble real de Java más simple y que permite stubs con fechas exactas.

   Lo que sí se mockea es lo externo, lento, caro o impredecible: la base de datos (`RepositorioCuentas`), servicios externos (`ServicioAntifraude`) y SMS o correo (`Notificador`).