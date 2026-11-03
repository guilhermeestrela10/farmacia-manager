package com.farmacia.farmacia.security;

import com.farmacia.farmacia.model.Perfil;
import com.farmacia.farmacia.model.Usuario;
import com.farmacia.farmacia.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final String nome;
    private final String login;
    private final String senha;

    public AdminInicial(UsuarioRepository repository,
                        PasswordEncoder encoder,
                        @Value("${app.admin.nome:Administrador}") String nome,
                        @Value("${app.admin.login:}") String login,
                        @Value("${app.admin.senha:}") String senha) {
        this.repository = repository;
        this.encoder = encoder;
        this.nome = nome;
        this.login = login;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }

        String loginNormalizado = login.trim().toLowerCase(Locale.ROOT);
        String nomeNormalizado = nome.trim();
        if (!loginNormalizado.matches("[a-z0-9._-]{3,50}")
                || nomeNormalizado.isBlank() || nomeNormalizado.length() > 100
                || senha.isBlank() || senha.length() < 8 || senha.length() > 72
                || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            log.warn("Nenhum usuário cadastrado. Configure app.admin.nome (1 a 100 caracteres), "
                    + "app.admin.login (3 a 50: letras, números, ponto, hífen ou sublinhado) "
                    + "e app.admin.senha (8 a 72 caracteres e até 72 bytes).");
            return;
        }

        repository.save(new Usuario(
                nomeNormalizado,
                loginNormalizado,
                encoder.encode(senha),
                Perfil.ADMIN));

        log.info("Administrador inicial criado: {}", loginNormalizado);
    }
}
