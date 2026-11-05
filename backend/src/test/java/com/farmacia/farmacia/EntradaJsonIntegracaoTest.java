package com.farmacia.farmacia;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import com.farmacia.farmacia.dto.EntradaEstoqueRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "farmacia.test.url", matches = "jdbc:postgresql://127\\.0\\.0\\.1:55432/postgres")
class EntradaJsonIntegracaoTest {
    @Autowired WebApplicationContext context;
    @Autowired JsonMapper mapper;

    private String entrada(String id, String quantidade) {
        return "{\"medicamentoId\":" + id + ",\"numeroLote\":\"TESTE\",\"validade\":\""
                + LocalDate.now().plusDays(1) + "\",\"quantidade\":" + quantidade + ",\"responsavel\":\"Teste\"}";
    }

    @Test
    void rejeitaQuantidadeFracionariaAntesDeConsultarEstoque() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        var resposta = mvc.perform(post("/estoque/entradas").contentType("application/json")
                .content(entrada("9223372036854775000", "1.9"))).andReturn().getResponse();
        assertEquals(400, resposta.getStatus());
        assertTrue(resposta.getContentAsString().contains("mensagem"));
    }

    @Test
    void rejeitaIdFracionarioEMantemInteirosValidos() {
        assertThrows(Exception.class, () -> mapper.readValue(entrada("1.9", "1"), EntradaEstoqueRequest.class));
        assertEquals(1, mapper.readValue(entrada("1", "1"), EntradaEstoqueRequest.class).quantidade());
    }
}
