package com.farmacia.farmacia.security;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TentativasDeLoginTest {
    @Test
    void bloqueiaNaQuintaFalhaNormalizaLoginELimpa() {
        var tentativas = new TentativasDeLogin();
        for (int i = 0; i < 4; i++) tentativas.registrarFalha(" VENDEDOR1 ");
        assertFalse(tentativas.bloqueado("vendedor1"));
        tentativas.registrarFalha("VENDEDOR1");
        assertTrue(tentativas.bloqueado("vendedor1"));
        assertFalse(tentativas.bloqueado("outro"));
        tentativas.limpar(" Vendedor1 ");
        assertFalse(tentativas.bloqueado("vendedor1"));
    }

    @Test
    void expiraDepoisDeQuinzeMinutosSemRenovarAoConsultar() {
        Instant inicio = Instant.parse("2026-11-03T12:00:00Z");
        var tentativas = new TentativasDeLogin(Clock.fixed(inicio, ZoneOffset.UTC));
        for (int i = 0; i < 5; i++) tentativas.registrarFalha("vendedor1");
        assertTrue(tentativas.bloqueado("vendedor1"));
        assertFalse(tentativas.bloqueado("vendedor1", inicio.plusSeconds(900)));
        assertFalse(tentativas.bloqueado("vendedor1"));
    }
}
