package com.farmacia.farmacia.security;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TentativasDeLogin {
    private record Tentativa(int falhas, Instant ate) {}
    private final Map<String, Tentativa> tentativas = new HashMap<>();
    private final Clock relogio;

    public TentativasDeLogin() { this(Clock.systemUTC()); }

    TentativasDeLogin(Clock relogio) { this.relogio = relogio; }

    public boolean bloqueado(String login) { return bloqueado(login, relogio.instant()); }

    synchronized boolean bloqueado(String login, Instant agora) {
        String chave = normalizar(login);
        Tentativa tentativa = tentativas.get(chave);
        if (tentativa == null) return false;
        if (!agora.isBefore(tentativa.ate())) {
            tentativas.remove(chave);
            return false;
        }
        return tentativa.falhas() >= 5;
    }

    public synchronized void registrarFalha(String login) {
        Instant agora = relogio.instant();
        tentativas.entrySet().removeIf(e -> !agora.isBefore(e.getValue().ate()));
        String chave = normalizar(login);
        Tentativa anterior = tentativas.get(chave);
        if (anterior != null && anterior.falhas() >= 5) return;
        int falhas = anterior == null ? 1 : anterior.falhas() + 1;
        // shortcut: contador em memória; persistir no banco antes de usar múltiplas instâncias.
        tentativas.put(chave, new Tentativa(falhas, agora.plus(Duration.ofMinutes(15))));
    }

    public synchronized void limpar(String login) { tentativas.remove(normalizar(login)); }

    private String normalizar(String login) {
        return login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
    }
}
