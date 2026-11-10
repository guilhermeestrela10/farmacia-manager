package com.farmacia.farmacia;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.farmacia.farmacia.dto.CategoriaRequest;
import com.farmacia.farmacia.model.TipoAuditoria;
import com.farmacia.farmacia.repository.CategoriaRepository;
import com.farmacia.farmacia.security.AuditoriaFilter;
import com.farmacia.farmacia.service.AuditoriaService;
import com.farmacia.farmacia.service.CategoriaService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "farmacia.test.mongo-uri", matches = "mongodb://127\\.0\\.0\\.1:55433/\\?replicaSet=farmacia-teste")
class AuditoriaTransacionalIntegracaoTest {
    @Autowired CategoriaService categorias;
    @Autowired CategoriaRepository repository;
    @Autowired org.springframework.transaction.PlatformTransactionManager transacoes;
    @Autowired AuditoriaService auditoriaReal;
    @Autowired com.farmacia.farmacia.repository.AuditoriaRepository registros;

    @Test
    void falhaNaAuditoriaNaoConfirmaCadastroNemRespostaDeSucesso() throws Exception {
        String nome = "Auditoria " + UUID.randomUUID();
        var auditoria = mock(AuditoriaService.class);
        doThrow(new IllegalStateException("Falha de auditoria simulada")).when(auditoria)
                .registrarAlteracao(anyString(), anyString(), anyString(), anyString(), anyInt(), anyString());
        var filtro = new AuditoriaFilter(auditoria, transacoes);
        var resposta = new MockHttpServletResponse();
        assertThrows(RuntimeException.class, () -> filtro.doFilter(new MockHttpServletRequest("POST", "/categorias"), resposta,
                (req, res) -> {
                    categorias.criar(new CategoriaRequest(nome));
                    res.getWriter().write("sucesso");
                    res.flushBuffer();
                }));
        assertFalse(repository.existsByNomeIgnoreCase(nome));
        assertFalse(resposta.getContentAsString().contains("sucesso"));
    }

    @Test
    void cadastroEAuditoriaConfirmamJuntosELiberamResposta() throws Exception {
        String nome = "Confirmado " + UUID.randomUUID();
        long antes = registros.count();
        var resposta = new MockHttpServletResponse();
        new AuditoriaFilter(auditoriaReal, transacoes).doFilter(new MockHttpServletRequest("POST", "/categorias"), resposta,
                (req, res) -> {
                    categorias.criar(new CategoriaRequest(nome));
                    ((jakarta.servlet.http.HttpServletResponse) res).setStatus(201);
                    res.getWriter().write("sucesso");
                    res.flushBuffer();
                });
        assertTrue(repository.existsByNomeIgnoreCase(nome));
        assertEquals(antes + 1, registros.count());
        assertEquals(201, resposta.getStatus());
        assertEquals("sucesso", resposta.getContentAsString());
    }

    @Test
    void recusaReverteAlteracaoMasMantemRegistroDeAuditoria() throws Exception {
        String nome = "Recusado " + UUID.randomUUID();
        long antes = registros.count();
        var resposta = new MockHttpServletResponse();
        new AuditoriaFilter(auditoriaReal, transacoes).doFilter(new MockHttpServletRequest("POST", "/categorias"), resposta,
                (req, res) -> {
                    categorias.criar(new CategoriaRequest(nome));
                    ((jakarta.servlet.http.HttpServletResponse) res).setStatus(409);
                });
        assertFalse(repository.existsByNomeIgnoreCase(nome));
        assertEquals(antes + 1, registros.count());
        assertEquals(409, resposta.getStatus());
    }
}
