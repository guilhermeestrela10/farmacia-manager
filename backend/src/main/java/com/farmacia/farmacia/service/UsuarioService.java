package com.farmacia.farmacia.service;

import com.farmacia.farmacia.dto.MeResponse;
import com.farmacia.farmacia.dto.UsuarioRequest;
import com.farmacia.farmacia.dto.UsuarioResponse;
import com.farmacia.farmacia.model.Perfil;
import com.farmacia.farmacia.model.Usuario;
import com.farmacia.farmacia.repository.UsuarioRepository;
import com.farmacia.farmacia.security.TentativasDeLogin;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    // Limite do formato de proteção das senhas (BCrypt), contado em bytes.
    private static final int MAXIMO_BYTES_SENHA = 72;

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final TentativasDeLogin tentativas;

    public UsuarioService(UsuarioRepository repository,
                          PasswordEncoder encoder,
                          TentativasDeLogin tentativas) {
        this.repository = repository;
        this.encoder = encoder;
        this.tentativas = tentativas;
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        String login = request.login().trim().toLowerCase(Locale.ROOT);

        if (repository.existsByLogin(login)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe um usuário com esse login.");
        }

        validarTamanhoDaSenha(request.senha());

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

    /**
     * O administrador define uma nova senha para outro usuário (ou para si mesmo).
     * As sessões abertas dessa pessoa deixam de valer.
     */
    @Transactional
    public void redefinirSenha(Long id, String novaSenha) {
        validarTamanhoDaSenha(novaSenha);

        Usuario usuario = buscar(id);
        if (encoder.matches(novaSenha, usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A nova senha deve ser diferente da atual.");
        }
        usuario.definirSenha(encoder.encode(novaSenha));
    }

    /**
     * Cada pessoa troca a própria senha, informando a atual.
     */
    @Transactional
    public void trocarPropriaSenha(String login, String senhaAtual, String novaSenha) {
        if (tentativas.bloqueado(login)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas. Aguarde alguns minutos e tente de novo.");
        }

        Usuario usuario = repository.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Sessão inválida. Entre novamente."));

        if (!encoder.matches(senhaAtual, usuario.getSenhaHash())) {
            tentativas.registrarFalha(login);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "A senha atual está incorreta.");
        }

        validarTamanhoDaSenha(novaSenha);

        if (encoder.matches(novaSenha, usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A nova senha deve ser diferente da atual.");
        }

        usuario.definirSenha(encoder.encode(novaSenha));
        tentativas.limpar(login);
    }

    @Transactional(readOnly = true)
    public MeResponse buscarPorLogin(String login) {
        Usuario usuario = repository.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Sessão inválida. Entre novamente."));

        return new MeResponse(usuario.getNome(), usuario.getLogin(), usuario.getPerfil());
    }

    private void validarTamanhoDaSenha(String senha) {
        if (senha.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES_SENHA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A senha é longa demais. Use até " + MAXIMO_BYTES_SENHA
                            + " caracteres comuns (acentos e emojis ocupam mais espaço).");
        }
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
