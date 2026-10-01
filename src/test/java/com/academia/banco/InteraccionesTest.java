package com.academia.banco;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

@DisplayName("MP-3 · Interacciones: verificar qué llamadas ocurrieron")
class InteraccionesTest {

    private RepositorioCuentas repositorio;
    private ServicioAntifraude antifraude;
    private Notificador notificador;
    private CajeroAutomatico cajero;
    private CuentaBancaria cuenta;

    @BeforeEach
    void preparar() {
        repositorio = mock(RepositorioCuentas.class);
        antifraude = mock(ServicioAntifraude.class);        // sin configurar: dice false (no sospechosa)
        notificador = mock(Notificador.class);
        cajero = new CajeroAutomatico(repositorio, antifraude, notificador, Clock.systemDefaultZone());
        cuenta = new CuentaBancaria("Ana");
        cuenta.depositar(new BigDecimal("2000.00"));

        // La cuenta "001" existe y hoy no se ha retirado nada.
        when(repositorio.buscar("001")).thenReturn(Optional.of(cuenta));
        when(repositorio.totalRetiradoEn(eq("001"), any(LocalDate.class))).thenReturn(new BigDecimal("0.00"));
    }

    // ── PRUEBA 1: guardar se llama exactamente una vez, con esos argumentos ──
    @Test
    @DisplayName("un retiro guarda la cuenta exactamente una vez")
    void retiroGuardaUnaVez() {
         cajero.retirar("001", new BigDecimal("500.00"));
        
         verify(repositorio).guardar("001", cuenta);
    }

    // ── PRUEBA 2: atrapar el texto del SMS con un ArgumentCaptor ──
    @Test
    @DisplayName("el SMS del retiro lleva el monto y el saldo nuevo")
    void contenidoDelSms() {
         cajero.retirar("001", new BigDecimal("500.00"));
        
         ArgumentCaptor<String> m = ArgumentCaptor.forClass(String.class);
         verify(notificador).enviarSms(eq("001"), m.capture());
         assertTrue(m.getValue().contains("500.00"));
         assertTrue(m.getValue().contains("1500.00"));
    }

    // ── PRUEBA 3: operación sospechosa, no se guarda ni se manda SMS ──
    @Test
    @DisplayName("una operación sospechosa no guarda ni manda SMS")
    void sospechosaNoGuardaNiNotifica() {
         when(antifraude.esSospechosa("001", new BigDecimal("500.00"))).thenReturn(true);
        
         assertThrows(OperacionRechazadaException.class,
                 () -> cajero.retirar("001", new BigDecimal("500.00")));
        
         verify(repositorio, never()).guardar(any(), any());
         verifyNoInteractions(notificador);
    }

    // ── PRUEBA 4: cuenta inexistente, ni antifraude ni notificador ──
    @Test
    @DisplayName("si la cuenta no existe, no se consulta el antifraude ni se manda SMS")
    void cuentaInexistenteSinInteracciones() {
         assertThrows(CuentaNoEncontradaException.class,
                 () -> cajero.retirar("999", new BigDecimal("500.00")));
        
         verifyNoInteractions(antifraude, notificador);
    }

    // ── PRUEBA 5: el orden: antifraude, después guardar, después SMS ──
    @Test
    @DisplayName("el orden es: antifraude, después guardar, después SMS")
    void ordenDeLasLlamadas() {
         cajero.retirar("001", new BigDecimal("500.00"));
        
         InOrder orden = inOrder(antifraude, repositorio, notificador);
         orden.verify(antifraude).esSospechosa("001", new BigDecimal("500.00"));
         orden.verify(repositorio).guardar("001", cuenta);
         orden.verify(notificador).enviarSms(eq("001"), anyString());
    }

    // ── PRUEBA 6 (C7): si el SMS falla, el retiro ya se hizo y no se deshace ──
    @Test
    @DisplayName("si el SMS falla, el retiro ya se hizo y no se deshace")
    void smsFallaPeroRetiroSeMantiene() {
         doThrow(new ServicioNoDisponibleException("SMS")).when(notificador).enviarSms(anyString(), anyString());
        
         assertDoesNotThrow(() -> cajero.retirar("001", new BigDecimal("500.00")));
        
         assertEquals(new BigDecimal("1500.00"), cajero.consultarSaldo("001"));
         verify(repositorio).guardar("001", cuenta);
    }

    // ── PRUEBA 7 (C5): sin saldo suficiente, no se guarda ni se manda SMS ──
    @Test
    @DisplayName("si la cuenta no tiene saldo, no se guarda ni se manda SMS")
    void saldoInsuficienteNoGuardaNiNotifica() {
         assertThrows(SaldoInsuficienteException.class,
                 () -> cajero.retirar("001", new BigDecimal("3000.00")));
        
         verify(repositorio, never()).guardar(any(), any());
         verifyNoInteractions(notificador);
    }
}