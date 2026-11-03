package com.farmacia.farmacia.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.farmacia.farmacia.model.Perfil;
import com.farmacia.farmacia.model.Usuario;
import com.farmacia.farmacia.repository.UsuarioRepository;
import com.farmacia.farmacia.security.TentativasDeLogin;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class UsuarioServiceTest {
    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final TentativasDeLogin tentativas = new TentativasDeLogin();
    private final Usuario usuario = new Usuario("Vendedor fictício", "vendedor1", encoder.encode("Exemplo123!"), Perfil.VENDEDOR);
    private final UsuarioService service = new UsuarioService(repository, encoder, tentativas);

    @Test
    void administradorNaoPodeRedefinirParaAMesmaSenha() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        assertThrows(ResponseStatusException.class, () -> service.redefinirSenha(1L, "Exemplo123!"));
        assertNull(usuario.getSenhaAlteradaEm());
    }

    @Test
    void trocaValidaAtualizaHashEMomentoSemGuardarSenhaAberta() {
        when(repository.findByLogin("vendedor1")).thenReturn(Optional.of(usuario));
        service.trocarPropriaSenha("vendedor1", "Exemplo123!", "OutraSenha123!");
        assertTrue(encoder.matches("OutraSenha123!", usuario.getSenhaHash()));
        assertNotEquals("OutraSenha123!", usuario.getSenhaHash());
        assertNotNull(usuario.getSenhaAlteradaEm());
    }

    @Test
    void cincoSenhasAtuaisIncorretasBloqueiamATroca() {
        when(repository.findByLogin("vendedor1")).thenReturn(Optional.of(usuario));
        for (int i = 0; i < 5; i++) assertThrows(ResponseStatusException.class,
                () -> service.trocarPropriaSenha("vendedor1", "Errada123!", "OutraSenha123!"));
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.trocarPropriaSenha("vendedor1", "Exemplo123!", "OutraSenha123!"));
        assertEquals(429, erro.getStatusCode().value());
        assertNull(usuario.getSenhaAlteradaEm());
    }

    @Test
    void rejeitaSenhaComMaisDeSetentaEDoisBytes() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.redefinirSenha(1L, "á".repeat(37)));
        assertEquals(400, erro.getStatusCode().value());
        assertNull(usuario.getSenhaAlteradaEm());
    }
}
