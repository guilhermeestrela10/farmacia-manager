package com.farmacia.farmacia;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.sql.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class MigracoesSegurancaTest {
    @Test
    void instalaBancoVazioEImpedeAlterarAuditoria() throws Exception {
        String url = System.getProperty("farmacia.test.url", "");
        // Usar somente instância descartável iniciada pelo roteiro de testes.
        assumeTrue(url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55432/postgres"));
        String schema = "seguranca_" + java.util.UUID.randomUUID().toString().replace("-", "");
        var flyway = Flyway.configure().dataSource(url, "farmacia_teste", "").schemas(schema).defaultSchema(schema).load();
        assertEquals(9, flyway.migrate().migrationsExecuted);
        flyway.validate();
        try (var c = DriverManager.getConnection(url, "farmacia_teste", ""); var s = c.createStatement()) {
            c.setSchema(schema);
            s.executeUpdate("INSERT INTO categoria (nome) VALUES ('Categoria fictícia')");
            assertThrows(SQLException.class, () -> s.executeUpdate("INSERT INTO categoria (nome) VALUES ('CATEGORIA FICTÍCIA')"));
            s.executeUpdate("INSERT INTO fabricante (nome) VALUES ('Fabricante fictício')");
            assertThrows(SQLException.class, () -> s.executeUpdate("INSERT INTO fabricante (nome) VALUES ('FABRICANTE FICTÍCIO')"));
            s.executeUpdate("INSERT INTO auditoria (tipo, usuario, acao, metodo, caminho, status, ip) VALUES ('ACESSO','ficticio','Login','POST','/login',200,'127.0.0.1')");
            for (String sql : new String[]{"UPDATE auditoria SET acao='Alterada'", "DELETE FROM auditoria", "TRUNCATE auditoria"}) {
                assertThrows(SQLException.class, () -> s.executeUpdate(sql));
            }
            try (var r = s.executeQuery("SELECT count(*) FROM auditoria")) { r.next(); assertEquals(1, r.getInt(1)); }
            try (var r = s.executeQuery("SELECT tipo FROM medicamento LIMIT 0")) { assertNotNull(r.getMetaData()); }
            try (var r = s.executeQuery("SELECT senha_alterada_em FROM usuario LIMIT 0")) { assertNotNull(r.getMetaData()); }
            try (var r = s.executeQuery("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid='movimentacao_estoque'::regclass AND conname='ck_movimentacao_tipo'")) {
                assertTrue(r.next());
                assertTrue(r.getString(1).contains("ESTORNO"));
            }
        }
    }
}
