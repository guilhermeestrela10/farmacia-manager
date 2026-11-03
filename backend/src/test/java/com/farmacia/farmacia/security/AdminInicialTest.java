package com.farmacia.farmacia.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.farmacia.farmacia.model.Perfil;
import com.farmacia.farmacia.model.Usuario;
import com.farmacia.farmacia.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdminInicialTest {
    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Test
    void rejeitaSenhasForaDosLimitesAntesDeCodificar() {
        for (String senha : new String[] {"curta", " ".repeat(8), "a".repeat(73), "á".repeat(37)}) {
            assertDoesNotThrow(() -> new AdminInicial(repository, encoder, "Admin", "admin", senha).run(null));
        }
        verify(repository, never()).save(any());
    }

    @Test
    void aceitaSetentaEDoisBytesENormalizaCadastro() {
        String senha = "á".repeat(36);
        new AdminInicial(repository, encoder, " Admin ", " ADMIN ", senha).run(null);
        var cadastro = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(cadastro.capture());
        assertEquals("Admin", cadastro.getValue().getNome());
        assertEquals("admin", cadastro.getValue().getLogin());
        assertEquals(Perfil.ADMIN, cadastro.getValue().getPerfil());
        assertTrue(encoder.matches(senha, cadastro.getValue().getSenhaHash()));
    }

    @Test
    void rejeitaLoginInvalidoENomeForaDosLimites() {
        for (String login : new String[] {"", "ab", "a".repeat(51), "admin@teste"}) {
            new AdminInicial(repository, encoder, "Admin", login, "Exemplo123!").run(null);
        }
        for (String nome : new String[] {" ", "a".repeat(101)}) {
            new AdminInicial(repository, encoder, nome, "admin", "Exemplo123!").run(null);
        }
        verify(repository, never()).save(any());
    }

    @Test
    void naoCriaAdministradorQuandoJaExistemUsuarios() {
        when(repository.count()).thenReturn(1L);
        new AdminInicial(repository, encoder, "Admin", "admin", "Exemplo123!").run(null);
        verify(repository, never()).save(any());
    }
}
