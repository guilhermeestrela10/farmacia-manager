package com.farmacia.farmacia;

import static org.junit.jupiter.api.Assertions.*;
import com.farmacia.farmacia.model.*;
import com.farmacia.farmacia.repository.*;
import com.mongodb.MongoWriteException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "farmacia.test.mongo-uri", matches = "mongodb://127\\.0\\.0\\.1:55433/\\?replicaSet=farmacia-teste")
class MigracoesSegurancaTest {
    @Autowired MongoTemplate mongo;
    @Autowired CategoriaRepository categorias;
    @Autowired FabricanteRepository fabricantes;
    @Autowired MedicamentoRepository medicamentos;
    @Autowired UsuarioRepository usuarios;
    @Autowired LoteRepository lotes;
    @Autowired AuditoriaRepository auditoria;

    @Test
    void indicesImpedemDuplicadosComAsMesmasRegrasDoContrato() {
        String sufixo = UUID.randomUUID().toString();
        var categoria = categorias.save(new Categoria("Categoria " + sufixo));
        assertThrows(DuplicateKeyException.class, () -> categorias.save(new Categoria(categoria.getNome().toUpperCase(Locale.ROOT))));
        var fabricante = fabricantes.save(new Fabricante("Fabricante " + sufixo));
        assertThrows(DuplicateKeyException.class, () -> fabricantes.save(new Fabricante(fabricante.getNome().toUpperCase(Locale.ROOT))));
        usuarios.save(new Usuario("Fictício", "indice-" + sufixo, "hash", Perfil.VENDEDOR));
        assertThrows(DuplicateKeyException.class, () -> usuarios.save(new Usuario("Outro", "indice-" + sufixo, "hash", Perfil.ADMIN)));
        var medicamento = medicamentos.save(new Medicamento("Fictício", null, categoria, fabricante,
                TipoMedicamento.GENERICO, new BigDecimal("0.10"), 0));
        lotes.save(new Lote(medicamento, "UNICO", LocalDate.now().plusDays(1)));
        assertThrows(DuplicateKeyException.class, () -> lotes.save(new Lote(medicamento, "UNICO", LocalDate.now().plusDays(2))));
    }

    @Test
    void idsNumericosSaoUnicosComInsercoesConcorrentes() throws Exception {
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var tarefas = java.util.stream.IntStream.range(0, 12)
                    .<java.util.concurrent.Callable<Long>>mapToObj(i -> () -> categorias.save(new Categoria("ID " + UUID.randomUUID())).getId())
                    .toList();
            var ids = new java.util.HashSet<Long>();
            for (var resultado : executor.invokeAll(tarefas, 20, java.util.concurrent.TimeUnit.SECONDS)) {
                Long id = resultado.get();
                assertTrue(id > 0);
                assertTrue(ids.add(id), "ID repetido: " + id);
            }
            assertEquals(12, ids.size());
        }
    }

    @Test
    void documentosPreservamIdsDecimaisReferenciasEVersoes() {
        String sufixo = UUID.randomUUID().toString();
        var categoria = categorias.save(new Categoria("Decimal " + sufixo));
        var fabricante = fabricantes.save(new Fabricante("Decimal " + sufixo));
        var medicamento = medicamentos.save(new Medicamento("Decimal", null, categoria, fabricante,
                TipoMedicamento.GENERICO, new BigDecimal("99999999.99"), 0));
        var documento = mongo.getCollection("medicamento").find(new Document("_id", medicamento.getId())).first();
        assertNotNull(documento);
        assertInstanceOf(Long.class, documento.get("_id"));
        assertTrue(medicamento.getId() > 0);
        assertEquals(0, documento.get("preco", Decimal128.class).bigDecimalValue().compareTo(new BigDecimal("99999999.99")));
        assertNotNull(documento.get("tipo"));
        var usuario = usuarios.save(new Usuario("Versão", "versao-" + sufixo, "hash", Perfil.VENDEDOR));
        usuario.definirSenha("novo-hash");
        usuarios.save(usuario);
        assertNotNull(mongo.getCollection("usuario").find(new Document("_id", usuario.getId())).first().get("senhaAlteradaEm"));
        var antigo = usuarios.findById(usuario.getId()).orElseThrow();
        usuario.desativar();
        usuarios.save(usuario);
        antigo.desativar();
        assertThrows(OptimisticLockingFailureException.class, () -> usuarios.save(antigo));
        org.springframework.test.util.ReflectionTestUtils.setField(categoria, "nome", "Renomeada " + sufixo);
        categorias.save(categoria);
        assertEquals(categoria.getNome(), medicamentos.findById(medicamento.getId()).orElseThrow().getCategoria().getNome());
    }

    @Test
    void esquemaRejeitaDocumentosInvalidosEAuditoriaPermaneceSomenteAcrescimo() {
        var registro = auditoria.save(new RegistroAuditoria(TipoAuditoria.ACESSO, "ficticio", "Login", "POST", "/login", 200, "127.0.0.1"));
        long antes = auditoria.count();
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> auditoria.save(registro));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> auditoria.delete(registro));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> auditoria.deleteById(registro.getId()));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> auditoria.deleteAll(java.util.List.of(registro)));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> auditoria.deleteAll());
        assertEquals(antes, auditoria.count());
        assertEquals("Login", auditoria.findById(registro.getId()).orElseThrow().getAcao());
        var invalido = new Document("_id", Long.MAX_VALUE).append("status", 42);
        assertEquals(121, assertThrows(MongoWriteException.class, () -> mongo.getCollection("auditoria").insertOne(invalido)).getCode());
        assertEquals(121, assertThrows(MongoWriteException.class, () -> mongo.getCollection("lote").insertOne(new Document("_id", Long.MAX_VALUE).append("quantidadeAtual", -1))).getCode());
        assertTrue(java.util.Arrays.asList(TipoMovimentacao.values()).contains(TipoMovimentacao.ESTORNO));
    }
}
