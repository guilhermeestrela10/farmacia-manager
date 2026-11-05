package com.farmacia.farmacia.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.farmacia.farmacia.model.Perfil;
import com.farmacia.farmacia.model.Usuario;
import com.farmacia.farmacia.repository.UsuarioRepository;
import com.farmacia.farmacia.service.AuditoriaService;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

class SecurityConfigTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private CsrfToken csrf;
    private MockHttpSession sessao;

    @Configuration
    @Import({SecurityConfig.class, UsuarioDetailsService.class, TentativasDeLogin.class, AuditoriaDeAcessos.class})
    static class Config {
        @Bean UsuarioRepository usuarios() { return mock(UsuarioRepository.class); }
        @Bean AuditoriaService auditoria() { return mock(AuditoriaService.class); }
        @Bean org.springframework.transaction.PlatformTransactionManager transacoes() {
            var manager = mock(org.springframework.transaction.PlatformTransactionManager.class);
            when(manager.getTransaction(any())).thenAnswer(invocacao -> new org.springframework.transaction.support.SimpleTransactionStatus());
            return manager;
        }
    }

    @RestController
    static class Paginas {
        @GetMapping("/login.html") String login() { return "login"; }
        @PostMapping("/usuarios") String criar() { return "ok"; }
        @GetMapping("/gestao/auditoria") String auditoria() { return "ok"; }
    }

    @BeforeEach
    void preparar() throws Exception {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Config.class);
        context.refresh();
        var encoder = context.getBean(org.springframework.security.crypto.password.PasswordEncoder.class);
        var usuario = new Usuario("Vendedor fictício", "vendedor1", encoder.encode("Exemplo123!"), Perfil.VENDEDOR);
        when(context.getBean(UsuarioRepository.class).findByLogin(anyString())).thenAnswer(invocacao ->
                "vendedor1".equals(invocacao.getArgument(0)) ? Optional.of(usuario) : Optional.empty());
        mvc = MockMvcBuilders.standaloneSetup(new Paginas()).addFilters(context.getBean(FilterChainProxy.class)).build();
        var pagina = mvc.perform(get("/login.html")).andReturn();
        csrf = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        assertNotNull(csrf);
        sessao = new MockHttpSession();
    }

    @AfterEach
    void fechar() { if (context != null) context.close(); }

    private ResultActions login(String usuario, String senha) throws Exception {
        return mvc.perform(post("/login").session(sessao)
                .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", csrf.getToken()))
                .header(csrf.getHeaderName(), csrf.getToken())
                .param("username", usuario).param("password", senha));
    }

    @Test
    void encoderCompartilhadoRecusaSenhasQueExcedemSetentaEDoisBytes() {
        var encoder = context.getBean(org.springframework.security.crypto.password.PasswordEncoder.class);
        String senhaAcentuada = "á".repeat(36);
        String hashAcentuado = encoder.encode(senhaAcentuada);
        assertTrue(encoder.matches(senhaAcentuada, hashAcentuado));
        assertFalse(encoder.matches(senhaAcentuada + "x", hashAcentuado));
        String senhaAscii = "a".repeat(72);
        String hashAscii = encoder.encode(senhaAscii);
        assertTrue(encoder.matches(senhaAscii, hashAscii));
        assertFalse(encoder.matches(senhaAscii + "x", hashAscii));
        assertFalse(encoder.matches(null, hashAscii));
    }

    @Test
    void loginComSufixoAlemDoLimiteContaComoFalha() throws Exception {
        var encoder = context.getBean(org.springframework.security.crypto.password.PasswordEncoder.class);
        String senha = "á".repeat(36);
        var usuario = new Usuario("Vendedor fictício", "vendedor1", encoder.encode(senha), Perfil.VENDEDOR);
        when(context.getBean(UsuarioRepository.class).findByLogin("vendedor1")).thenReturn(Optional.of(usuario));
        for (int i = 0; i < 4; i++) login("vendedor1", senha + "x").andExpect(status().isUnauthorized());
        login("vendedor1", senha + "x").andExpect(status().isTooManyRequests());
        assertNull(sessao.getAttribute(UsuarioAtivoFilter.ATRIBUTO_LOGIN));
    }

    @Test
    void bloqueioImpedeLoginMesmoComSenhaCorreta() throws Exception {
        for (int i = 0; i < 4; i++) login("vendedor1", "Errada123!").andExpect(status().isUnauthorized());
        login("vendedor1", "Errada123!").andExpect(status().isTooManyRequests());
        login("vendedor1", "Exemplo123!").andExpect(status().isTooManyRequests());
        assertNull(sessao.getAttribute(UsuarioAtivoFilter.ATRIBUTO_LOGIN));
    }

    @Test
    void usuarioInexistenteRecebeMesmoBloqueio() throws Exception {
        for (int i = 0; i < 4; i++) login("inexistente", "Errada123!").andExpect(status().isUnauthorized());
        login("inexistente", "Errada123!").andExpect(status().isTooManyRequests());
    }

    @Test
    void vendedorRecebe403EmGestaoEUsuariosComCsrfValido() throws Exception {
        login("vendedor1", "Exemplo123!").andExpect(status().isOk());
        assertNotNull(sessao.getAttribute(UsuarioAtivoFilter.ATRIBUTO_LOGIN));
        var pagina = mvc.perform(get("/login.html").session(sessao)).andReturn();
        var token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(get("/gestao/auditoria").session(sessao)).andExpect(status().isForbidden());
        mvc.perform(post("/usuarios").session(sessao)
                .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", token.getToken()))
                .header(token.getHeaderName(), token.getToken())).andExpect(status().isForbidden());
        verify(context.getBean(AuditoriaService.class)).registrar(
                eq(com.farmacia.farmacia.model.TipoAuditoria.ALTERACAO), eq("vendedor1"),
                eq("Criar usuário"), eq("POST"), eq("/usuarios"), eq(403), eq("127.0.0.1"));
    }

    @Test
    void loginELogoutSaoAuditadosComIP() throws Exception {
        login("vendedor1", "Exemplo123!").andExpect(status().isOk());
        var pagina = mvc.perform(get("/login.html").session(sessao)).andReturn();
        var token = (CsrfToken) pagina.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/logout").session(sessao)
                .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", token.getToken()))
                .header(token.getHeaderName(), token.getToken())).andExpect(status().isOk());
        var auditoria = context.getBean(AuditoriaService.class);
        verify(auditoria).registrar(eq(com.farmacia.farmacia.model.TipoAuditoria.ACESSO),
                eq("vendedor1"), eq("Login"), eq("POST"), eq("/login"), eq(200), eq("127.0.0.1"));
        verify(auditoria).registrar(eq(com.farmacia.farmacia.model.TipoAuditoria.ACESSO),
                eq("vendedor1"), eq("Logout"), eq("POST"), eq("/logout"), eq(200), eq("127.0.0.1"));
    }
}
