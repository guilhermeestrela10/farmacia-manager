package com.farmacia.farmacia.security;

import com.farmacia.farmacia.model.Perfil;
import com.farmacia.farmacia.model.Usuario;
import com.farmacia.farmacia.repository.UsuarioRepository;
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

        if (login.isBlank() || senha.length() < 8) {
            log.warn("Nenhum usuário cadastrado. Defina app.admin.login e app.admin.senha "
                    + "(mínimo 8 caracteres) no application-local.properties.");
            return;
        }

        repository.save(new Usuario(
                nome.trim(),
                login.trim().toLowerCase(Locale.ROOT),
                encoder.encode(senha),
                Perfil.ADMIN));

        log.info("Administrador inicial criado: {}", login.trim().toLowerCase(Locale.ROOT));
    }
}