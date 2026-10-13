package com.farmacia.farmacia.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.farmacia.farmacia.dto.MeResponse;
import com.farmacia.farmacia.dto.UsuarioRequest;
import com.farmacia.farmacia.dto.UsuarioResponse;
import com.farmacia.farmacia.model.Perfil;
import com.farmacia.farmacia.model.Usuario;
import com.farmacia.farmacia.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        String login = request.login().trim().toLowerCase(Locale.ROOT);

        if (repository.existsByLogin(login)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe um usuário com esse login.");
        }

        Usuario usuario = repository.save(new Usuario(
                request.nome().trim(),
                login,
                encoder.encode(request.senha()),
                request.perfil()));

        return toResponse(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return repository.findAllByOrderByNome().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UsuarioResponse desativar(Long id, String loginLogado) {
        Usuario usuario = buscar(id);

        if (usuario.getLogin().equals(loginLogado)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Você não pode desativar o seu próprio usuário.");
        }

        if (usuario.getPerfil() == Perfil.ADMIN && usuario.isAtivo()
                && repository.countByPerfilAndAtivoTrue(Perfil.ADMIN) <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "É preciso manter ao menos um administrador ativo.");
        }

        usuario.desativar();
        return toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse ativar(Long id) {
        Usuario usuario = buscar(id);
        usuario.ativar();
        return toResponse(usuario);
    }

    @Transactional(readOnly = true)
    public MeResponse buscarPorLogin(String login) {
        Usuario usuario = repository.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Sessão inválida. Entre novamente."));

        return new MeResponse(usuario.getNome(), usuario.getLogin(), usuario.getPerfil());
    }

    private Usuario buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
                u.getId(), u.getNome(), u.getLogin(), u.getPerfil(), u.isAtivo());
    }
}  