package com.farmacia.farmacia;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import com.farmacia.farmacia.repository.*;
import com.farmacia.farmacia.security.DemoDados;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties={"spring.flyway.schemas=demo_integracao", "spring.flyway.default-schema=demo_integracao",
        "spring.jpa.properties.hibernate.default_schema=demo_integracao", "spring.datasource.hikari.schema=demo_integracao"})
@ActiveProfiles({"test","demo"})
@EnabledIfSystemProperty(named="farmacia.test.url", matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/postgres")
class DemoAcessoIntegracaoTest {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;
    @Autowired DemoDados dados;
    @Autowired UsuarioRepository usuarios;
    @Autowired MedicamentoRepository medicamentos;
    @Autowired VendaRepository vendas;

    @Test void seedEIdempotenteEContemCenariosReaisDoFluxo() {
        assertEquals(2,usuarios.count());
        assertEquals(12,medicamentos.count());
        assertEquals(19,vendas.count());
        dados.run(new DefaultApplicationArguments());
        assertEquals(19,vendas.count());
        assertEquals(12,medicamentos.count());
    }

    @Test void entradaDemoExigeCsrfEAplicaPerfil() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(filtros).build();
        assertEquals(200,mvc.perform(get("/health")).andReturn().getResponse().getStatus());
        assertEquals(200,mvc.perform(get("/demo/info")).andReturn().getResponse().getStatus());
        assertEquals(403,mvc.perform(post("/demo/entrar").contentType("application/json")
                .content("{\"perfil\":\"ADMIN\"}")).andReturn().getResponse().getStatus());
        for (String perfil:new String[]{"VENDEDOR","ADMIN"}) {
            var pagina=mvc.perform(get("/login.html")).andReturn();
            var token=(CsrfToken)pagina.getRequest().getAttribute(CsrfToken.class.getName());
            var resposta=mvc.perform(post("/demo/entrar").contentType("application/json")
                    .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN",token.getToken()))
                    .header(token.getHeaderName(),token.getToken()).content("{\"perfil\":\""+perfil+"\"}")).andReturn();
            assertEquals(200,resposta.getResponse().getStatus());
            var sessao=(MockHttpSession)resposta.getRequest().getSession(false);
            assertNotNull(sessao);
            assertEquals(200,mvc.perform(get("/clientes").session(sessao)).andReturn().getResponse().getStatus());
            assertEquals(perfil.equals("ADMIN")?200:403,mvc.perform(get("/gestao/auditoria").session(sessao))
                    .andReturn().getResponse().getStatus());
        }
    }
}
