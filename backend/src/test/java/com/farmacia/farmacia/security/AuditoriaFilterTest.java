package com.farmacia.farmacia.security;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.farmacia.farmacia.model.TipoAuditoria;
import com.farmacia.farmacia.service.AuditoriaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuditoriaFilterTest {
    @AfterEach
    void limpar() { SecurityContextHolder.clearContext(); }

    @Test
    void registraRecusaSemCorpoNemQueryString() throws Exception {
        var service = mock(AuditoriaService.class);
        var req = new MockHttpServletRequest("POST", "/usuarios/12/senha");
        req.setQueryString("senha=nao-registrar");
        req.setContent("{\"senha\":\"nao-registrar\"}".getBytes());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("vendedor1", null, java.util.List.of()));
        new AuditoriaFilter(service).doFilter(req, new MockHttpServletResponse(),
                (request, response) -> ((jakarta.servlet.http.HttpServletResponse) response).setStatus(403));
        verify(service).registrar(eq(TipoAuditoria.ALTERACAO), eq("vendedor1"),
                eq("Redefinir senha do usuário #12"), eq("POST"),
                eq("/usuarios/12/senha"), eq(403), eq("127.0.0.1"));
        verifyNoMoreInteractions(service);
    }

    @Test
    void naoRegistraConsultas() throws Exception {
        var service = mock(AuditoriaService.class);
        new AuditoriaFilter(service).doFilter(new MockHttpServletRequest("GET", "/gestao/auditoria"),
                new MockHttpServletResponse(), (request, response) -> {});
        verifyNoInteractions(service);
    }
}
