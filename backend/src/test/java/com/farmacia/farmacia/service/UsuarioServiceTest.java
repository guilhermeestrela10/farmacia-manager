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
        when(repository.buscarPorIdComTravamento(1L)).thenReturn(Optional.of(usuario));
        assertThrows(ResponseStatusException.class, () -> service.redefinirSenha(1L, "Exemplo123!"));
        assertNull(usuario.getSenhaAlteradaEm());
    }

    @Test
    void trocaValidaAtualizaHashEMomentoSemGuardarSenhaAberta() {
        when(repository.buscarPorLoginComTravamento("vendedor1")).thenReturn(Optional.of(usuario));
        service.trocarPropriaSenha("vendedor1", "Exemplo123!", "OutraSenha123!");
        assertTrue(encoder.matches("OutraSenha123!", usuario.getSenhaHash()));
        assertNotEquals("OutraSenha123!", usuario.getSenhaHash());
        assertNotNull(usuario.getSenhaAlteradaEm());
    }

    @Test
    void cincoSenhasAtuaisIncorretasBloqueiamATroca() {
        when(repository.buscarPorLoginComTravamento("vendedor1")).thenReturn(Optional.of(usuario));
        for (int i = 0; i < 5; i++) assertThrows(ResponseStatusException.class,
                () -> service.trocarPropriaSenha("vendedor1", "Errada123!", "OutraSenha123!"));
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.trocarPropriaSenha("vendedor1", "Exemplo123!", "OutraSenha123!"));
        assertEquals(429, erro.getStatusCode().value());
        assertNull(usuario.getSenhaAlteradaEm());
    }

    @Test
    void rejeitaSenhaComMaisDeSetentaEDoisBytes() {
        when(repository.buscarPorIdComTravamento(1L)).thenReturn(Optional.of(usuario));
        var erro = assertThrows(ResponseStatusException.class,
                () -> service.redefinirSenha(1L, "á".repeat(37)));
        assertEquals(400, erro.getStatusCode().value());
        assertNull(usuario.getSenhaAlteradaEm());
    }

    @Test
    void redefinicaoMantemUsuarioDesativado() {
        usuario.desativar();
        when(repository.buscarPorIdComTravamento(1L)).thenReturn(Optional.of(usuario));
        service.redefinirSenha(1L, "OutraSenha123!");
        assertFalse(usuario.isAtivo());
        assertTrue(encoder.matches("OutraSenha123!", usuario.getSenhaHash()));
        verify(repository, never()).findById(anyLong());
    }

    @Test
    void ativacaoPreservaSenhaAtual() {
        usuario.desativar();
        usuario.definirSenha(encoder.encode("OutraSenha123!"));
        var versao = usuario.getSenhaAlteradaEm();
        when(repository.buscarPorIdComTravamento(1L)).thenReturn(Optional.of(usuario));
        assertTrue(service.ativar(1L).ativo());
        assertTrue(encoder.matches("OutraSenha123!", usuario.getSenhaHash()));
        assertEquals(versao, usuario.getSenhaAlteradaEm());
    }

    @Test
    void bloqueiaAdministradoresAntesDoAlvoEConfereContagemAtual() {
        var administrador = new Usuario("Outro admin", "admin2", "hash-ficticio", Perfil.ADMIN);
        when(repository.buscarPorIdComTravamento(2L)).thenReturn(Optional.of(administrador));
        when(repository.countByPerfilAndAtivoTrue(Perfil.ADMIN)).thenReturn(1L);
        var erro = assertThrows(ResponseStatusException.class, () -> service.desativar(2L, "admin1"));
        assertEquals(409, erro.getStatusCode().value());
        assertTrue(administrador.isAtivo());
        var ordem = inOrder(repository);
        ordem.verify(repository).buscarAdministradoresAtivosComTravamento();
        ordem.verify(repository).buscarPorIdComTravamento(2L);
        ordem.verify(repository).countByPerfilAndAtivoTrue(Perfil.ADMIN);
    }

    @Test
    void permiteDesativarOutroAdministradorQuandoRestaraUmAtivo() {
        var administrador = new Usuario("Outro admin", "admin2", "hash-ficticio", Perfil.ADMIN);
        when(repository.buscarPorIdComTravamento(2L)).thenReturn(Optional.of(administrador));
        when(repository.countByPerfilAndAtivoTrue(Perfil.ADMIN)).thenReturn(2L);
        assertFalse(service.desativar(2L, "admin1").ativo());
    }

    @Test
    void impedeDesativarProprioUsuario() {
        when(repository.buscarPorIdComTravamento(1L)).thenReturn(Optional.of(usuario));
        var erro = assertThrows(ResponseStatusException.class, () -> service.desativar(1L, "vendedor1"));
        assertEquals(409, erro.getStatusCode().value());
        assertTrue(usuario.isAtivo());
    }
}
