package com.farmacia.farmacia.security;

import com.farmacia.farmacia.model.Usuario;
import java.time.Instant;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

public class UsuarioAutenticado extends User {
    private final Instant versaoSenha;

    public UsuarioAutenticado(Usuario usuario, boolean bloqueado) {
        super(usuario.getLogin(), usuario.getSenhaHash(), usuario.isAtivo(), true, true, !bloqueado,
                AuthorityUtils.createAuthorityList("ROLE_" + usuario.getPerfil().name()));
        // A versão precisa vir da mesma leitura do hash; horário do login permite corrida com redefinição.
        this.versaoSenha = usuario.getSenhaAlteradaEm() == null ? Instant.EPOCH : usuario.getSenhaAlteradaEm();
    }

    public Instant getVersaoSenha() { return versaoSenha; }
}
