package com.farmacia.farmacia.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.farmacia.farmacia.model.Perfil;
import com.farmacia.farmacia.model.Usuario;
import com.farmacia.farmacia.repository.UsuarioRepository;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class UsuarioAtivoFilterTest {
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }

    @Test
    void trocaDeSenhaEncerraTodasAsSessoesAntigas() throws Exception {
        var usuario = new Usuario("Fictício", "vendedor1", "hash-ficticio", Perfil.VENDEDOR);
        usuario.definirSenha("outro-hash-ficticio");
        var repository = mock(UsuarioRepository.class);
        when(repository.findByLogin("vendedor1")).thenReturn(Optional.of(usuario));
        var filtro = new UsuarioAtivoFilter(repository);
        for (int i = 0; i < 2; i++) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("vendedor1", null, java.util.List.of()));
            var req = new MockHttpServletRequest("GET", "/auth/me");
            var sessao = new MockHttpSession();
            sessao.setAttribute(UsuarioAtivoFilter.ATRIBUTO_LOGIN, usuario.getSenhaAlteradaEm().minusNanos(1));
            req.setSession(sessao);
            var res = new MockHttpServletResponse();
            AtomicBoolean prosseguiu = new AtomicBoolean();
            filtro.doFilter(req, res, (request, response) -> prosseguiu.set(true));
            assertEquals(401, res.getStatus());
            assertTrue(sessao.isInvalid());
            assertFalse(prosseguiu.get());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Test
    void novaSessaoContinuaValida() throws Exception {
        var usuario = new Usuario("Fictício", "vendedor1", "hash-ficticio", Perfil.VENDEDOR);
        usuario.definirSenha("outro-hash-ficticio");
        var repository = mock(UsuarioRepository.class);
        when(repository.findByLogin("vendedor1")).thenReturn(Optional.of(usuario));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("vendedor1", null, java.util.List.of()));
        var req = new MockHttpServletRequest("GET", "/auth/me");
        req.getSession().setAttribute(UsuarioAtivoFilter.ATRIBUTO_LOGIN, usuario.getSenhaAlteradaEm());
        AtomicBoolean prosseguiu = new AtomicBoolean();
        new UsuarioAtivoFilter(repository).doFilter(req, new MockHttpServletResponse(), (request, response) -> prosseguiu.set(true));
        assertTrue(prosseguiu.get());
    }

    @Test
    void invalidaSenhaAntigaMesmoQuandoSeuTimestampAntecedeOLogin() throws Exception {
        var usuario = new Usuario("Fictício", "vendedor1", "hash-antigo", Perfil.VENDEDOR);
        var repository = mock(UsuarioRepository.class);
        when(repository.findByLogin("vendedor1")).thenReturn(Optional.of(usuario));
        var detalhes = (UsuarioAutenticado) new UsuarioDetailsService(repository, new TentativasDeLogin())
                .loadUserByUsername("vendedor1");
        usuario.definirSenha("hash-novo");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(detalhes, null, detalhes.getAuthorities()));
        var req = new MockHttpServletRequest("GET", "/auth/me");
        req.getSession().setAttribute(UsuarioAtivoFilter.ATRIBUTO_LOGIN, detalhes.getVersaoSenha());
        var res = new MockHttpServletResponse();
        new UsuarioAtivoFilter(repository).doFilter(req, res, (request, response) -> fail("Senha antiga não pode manter sessão."));
        assertEquals(401, res.getStatus());
    }
}
