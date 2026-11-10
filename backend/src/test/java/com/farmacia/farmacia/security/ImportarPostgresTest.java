package com.farmacia.farmacia.security;

import static org.junit.jupiter.api.Assertions.*;
import com.mongodb.DBRef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;

class ImportarPostgresTest {
    private Map<String, List<Document>> origem() {
        Map<String, List<Document>> origem = new LinkedHashMap<>();
        for (String tabela : ImportarPostgres.TABELAS) origem.put(tabela, new ArrayList<>());
        origem.get("configuracao").add(new Document("id", 1L).append("nome_farmacia", "Teste"));
        return origem;
    }

    @Test void recusaOrigemForaDaDemoSemAbrirConexao() {
        assertDoesNotThrow(() -> ImportarPostgres.validarOrigem("dpg-dbmg7vmk1f9s7385rp5g-a.oregon-postgres.render.com", "farmacia_manager_demo_db", "5432"));
        assertThrows(IllegalStateException.class, () -> ImportarPostgres.validarOrigem("outro.render.com", "farmacia_manager_demo_db", "5432"));
        assertThrows(IllegalStateException.class, () -> ImportarPostgres.validarOrigem("dpg-dbmg7vmk1f9s7385rp5g-a.evil.example", "farmacia_manager_demo_db", "5432"));
        assertThrows(IllegalStateException.class, () -> ImportarPostgres.validarOrigem("dpg-dbmg7vmk1f9s7385rp5g-a", "outro_banco", "5432"));
        assertThrows(IllegalStateException.class, () -> ImportarPostgres.validarOrigem("dpg-dbmg7vmk1f9s7385rp5g-a", "farmacia_manager_demo_db", "0"));
    }

    @Test void preservaIdsReferenciasItensDecimaisEDatas() {
        var dados = origem();
        dados.get("categoria").add(new Document("id", 2L).append("nome", "Categoria"));
        dados.get("fabricante").add(new Document("id", 3L).append("nome", "Fabricante"));
        dados.get("usuario").add(new Document("id", 4L).append("senha_hash", "hash-ficticio"));
        dados.get("medicamento").add(new Document("id", 5L).append("categoria_id", 2L).append("fabricante_id", 3L).append("preco", new BigDecimal("0.10")));
        dados.get("lote").add(new Document("id", 6L).append("medicamento_id", 5L).append("validade", LocalDate.of(2030, 2, 1)).append("quantidade_atual", 3));
        dados.get("venda").add(new Document("id", 7L).append("vendedor_id", 4L).append("total", new BigDecimal("0.30")));
        dados.get("venda_item").add(new Document("id", 8L).append("venda_id", 7L).append("medicamento_id", 5L).append("quantidade", 3).append("preco_unitario", new BigDecimal("0.10")));
        var destino = ImportarPostgres.converter(dados);
        var venda = destino.get("venda").getFirst();
        assertEquals(7L, venda.get("_id"));
        assertEquals(new DBRef("usuario", 4L), venda.get("vendedor"));
        assertEquals(new Decimal128(new BigDecimal("0.30")), venda.get("total"));
        var item = venda.getList("itens", Document.class).getFirst();
        assertEquals(8L, item.get("_id"));
        assertEquals(new DBRef("medicamento", 5L), item.get("medicamento"));
        assertFalse(item.containsKey("venda"));
        assertFalse(item.containsKey("vendaId"));
        assertFalse(destino.containsKey("venda_item"));
        assertEquals("hash-ficticio", destino.get("usuario").getFirst().get("senhaHash"));
        assertEquals(Date.from(LocalDate.of(2030, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), destino.get("lote").getFirst().get("validade"));
    }

    @Test void recusaReferenciaQueNaoExisteETotalDivergente() {
        var dados = origem();
        dados.get("venda_item").add(new Document("id", 1L).append("venda_id", 99L).append("medicamento_id", 1L));
        var referenciasInvalidas = dados;
        assertThrows(IllegalStateException.class, () -> ImportarPostgres.converter(referenciasInvalidas));
        dados = origem();
        dados.get("usuario").add(new Document("id", 1L));
        dados.get("venda").add(new Document("id", 1L).append("vendedor_id", 1L).append("total", BigDecimal.TEN));
        var invalidos = dados;
        assertThrows(IllegalStateException.class, () -> ImportarPostgres.converter(invalidos));
    }

    @Test void fingerprintIgnoraOrdemDeCamposMasDetectaMudancaDeSaldo() {
        var um = new Document("id", 1L).append("saldo", 3);
        var dois = new Document("saldo", 3).append("id", 1L);
        assertEquals(ImportarPostgres.fingerprint(um), ImportarPostgres.fingerprint(dois));
        dois.put("saldo", 2);
        assertNotEquals(ImportarPostgres.fingerprint(um), ImportarPostgres.fingerprint(dois));
    }
}
