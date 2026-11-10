package com.farmacia.farmacia.config;

import com.farmacia.farmacia.model.*;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import jakarta.annotation.PostConstruct;
import java.util.*;
import org.bson.Document;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;

@Configuration
public class MongoPersistenceConfig {
    private static final List<String> COLLECTIONS = List.of("categoria", "fabricante", "medicamento", "usuario", "lote",
        "movimentacao_estoque", "venda", "cliente", "atendimento", "receita", "dispensacao", "configuracao", "auditoria");
    private final MongoTemplate mongo;
    @Value("${app.mongo.provisionar:true}")
    private boolean provisionar;
    public MongoPersistenceConfig(MongoTemplate mongo) { this.mongo = mongo; }

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory, TransactionOptions.builder().readConcern(ReadConcern.SNAPSHOT)
            .writeConcern(WriteConcern.MAJORITY).readPreference(ReadPreference.primary()).build());
    }

    @PostConstruct
    void initialize() {
        if (!provisionar) {
            if (mongo.findById(1L, Configuracao.class) == null) {
                throw new IllegalStateException("Banco MongoDB ainda não provisionado.");
            }
            return;
        }
        // Collections, counters and indexes must exist before any business transaction starts.
        for (String collection : COLLECTIONS) {
            if (!mongo.collectionExists(collection)) mongo.createCollection(collection);
            mongo.executeCommand(new Document("collMod", collection).append("validator", validator(collection))
                .append("validationLevel", "strict").append("validationAction", "error"));
        }
        for (String collection : List.of("_sequences", "_guards")) if (!mongo.collectionExists(collection)) mongo.createCollection(collection);
        for (String collection : COLLECTIONS) {
            var last = mongo.getCollection(collection).find().sort(new Document("_id", -1)).projection(new Document("_id", 1)).first();
            long maximum = last == null ? 0L : ((Number) last.get("_id")).longValue();
            sequence(collection, maximum);
        }
        embeddedSequence("venda", "venda_item");
        embeddedSequence("receita", "item_receita");
        mongo.upsert(Query.query(Criteria.where("_id").is("administradores")), new Update().setOnInsert("version", 0L), "_guards");
        mongo.upsert(Query.query(Criteria.where("_id").is(1L)), new Update().setOnInsert("nomeFarmacia", "Farmácias Ruanna").setOnInsert("version", 0L), "configuracao");
        indexes();
    }

    private void sequence(String collection, long value) {
        mongo.upsert(Query.query(Criteria.where("_id").is(collection)), new Update().max("value", value), "_sequences");
    }
    private void embeddedSequence(String parent, String collection) {
        var result = mongo.getCollection(parent).aggregate(List.of(new Document("$unwind", "$itens"),
            new Document("$group", new Document("_id", null).append("maximum", new Document("$max", "$itens._id"))))).first();
        sequence(collection, result == null || result.get("maximum") == null ? 0L : ((Number) result.get("maximum")).longValue());
    }
    private void index(String collection, Document keys, String name, boolean unique, Document options) {
        Document command = new Document("key", keys).append("name", name).append("unique", unique);
        if (options != null) command.putAll(options);
        mongo.executeCommand(new Document("createIndexes", collection).append("indexes", List.of(command)));
    }
    private void indexes() {
        for (String collection : List.of("categoria", "fabricante")) {
            index(collection, new Document("nome", 1), "uk_" + collection + "_nome_sem_caixa", true,
                new Document("collation", new Document("locale", "pt").append("strength", 2)));
        }
        index("usuario", new Document("login", 1), "uk_usuario_login", true, null);
        index("usuario", new Document("perfil", 1).append("ativo", 1), "idx_usuario_perfil_ativo", false, null);
        index("medicamento", new Document("codigoBarras", 1), "uk_medicamento_codigo_barras", true,
            new Document("partialFilterExpression", new Document("codigoBarras", new Document("$type", "string"))));
        index("medicamento", new Document("nome", 1), "idx_medicamento_nome", false, null);
        index("lote", new Document("medicamento.$id", 1).append("numeroLote", 1), "uk_lote_medicamento_numero", true, null);
        index("lote", new Document("medicamento.$id", 1).append("validade", 1).append("_id", 1), "idx_lote_fefo", false, null);
        index("lote", new Document("validade", 1), "idx_lote_validade", false, null);
        index("movimentacao_estoque", new Document("lote.$id", 1).append("dataHora", -1), "idx_movimentacao_lote_data", false, null);
        index("movimentacao_estoque", new Document("vendaId", 1).append("tipo", 1), "idx_movimentacao_venda", false, null);
        index("venda", new Document("criadaEm", -1).append("_id", -1), "idx_venda_criada_em", false, null);
        index("venda", new Document("vendedor.$id", 1), "idx_venda_vendedor", false, null);
        index("atendimento", new Document("cliente.$id", 1), "idx_atendimento_cliente", false, null);
        index("receita", new Document("cliente.$id", 1), "idx_receita_cliente", false, null);
        index("dispensacao", new Document("venda.$id", 1), "uk_dispensacao_venda", true, null);
        index("dispensacao", new Document("receita.$id", 1), "idx_dispensacao_receita", false, null);
        index("auditoria", new Document("dataHora", -1).append("_id", -1), "idx_auditoria_data", false, null);
        index("auditoria", new Document("usuario", 1), "idx_auditoria_usuario", false, null);
    }

    private static Document string(int length) { return new Document("bsonType", "string").append("maxLength", length); }
    private static Document nonblank(int length) { return string(length).append("pattern", "\\S"); }
    private static Document integer(int min, int max) { return new Document("bsonType", "int").append("minimum", min).append("maximum", max); }
    private static Document date() { return new Document("bsonType", "date"); }
    private static Document reference() { return new Document("bsonType", "object"); }
    private static Document money() { return new Document("bsonType", "decimal").append("minimum", 0).append("exclusiveMinimum", true); }
    private static Document enumeration(String... values) { return new Document("enum", Arrays.asList(values)); }
    private static Document validator(String collection) {
        Document props = new Document("_id", new Document("bsonType", "long").append("minimum", 1));
        List<String> required = new ArrayList<>(List.of("_id"));
        switch (collection) {
            case "categoria", "fabricante" -> { props.append("nome", string(collection.equals("categoria") ? 80 : 120)); required.add("nome"); }
            case "usuario" -> {
                props.append("nome", string(100)).append("login", string(50)).append("senhaHash", string(100))
                    .append("perfil", enumeration("ADMIN", "VENDEDOR")).append("ativo", new Document("bsonType", "bool")).append("criadoEm", date());
                required.addAll(List.of("nome", "login", "senhaHash", "perfil", "ativo", "criadoEm"));
            }
            case "medicamento" -> {
                props.append("nome", string(150)).append("categoria", reference()).append("fabricante", reference()).append("preco", money())
                    .append("tipo", enumeration("REFERENCIA", "GENERICO", "SIMILAR")).append("estoqueMinimo", integer(0, Integer.MAX_VALUE))
                    .append("ativo", new Document("bsonType", "bool")).append("criadoEm", date());
                required.addAll(List.of("nome", "categoria", "fabricante", "preco", "tipo", "estoqueMinimo", "ativo", "criadoEm"));
            }
            case "lote" -> {
                props.append("medicamento", reference()).append("numeroLote", string(50)).append("validade", date())
                    .append("quantidadeAtual", integer(0, Integer.MAX_VALUE)).append("criadoEm", date());
                required.addAll(List.of("medicamento", "numeroLote", "validade", "quantidadeAtual", "criadoEm"));
            }
            case "movimentacao_estoque" -> {
                props.append("lote", reference()).append("tipo", enumeration("ENTRADA", "SAIDA", "AJUSTE_ENTRADA", "AJUSTE_SAIDA", "PERDA", "ESTORNO"))
                    .append("quantidade", integer(1, Integer.MAX_VALUE)).append("responsavel", string(100)).append("dataHora", date());
                required.addAll(List.of("lote", "tipo", "quantidade", "responsavel", "dataHora"));
            }
            case "venda" -> {
                props.append("vendedor", reference()).append("formaPagamento", enumeration("DINHEIRO", "PIX", "DEBITO", "CREDITO"))
                    .append("total", money()).append("status", enumeration("CONCLUIDA", "CANCELADA")).append("criadaEm", date()).append("itens", items(true));
                required.addAll(List.of("vendedor", "formaPagamento", "total", "status", "criadaEm", "itens"));
            }
            case "cliente" -> { props.append("nome", nonblank(100)).append("ativo", new Document("bsonType", "bool")); required.addAll(List.of("nome", "ativo")); }
            case "atendimento" -> {
                props.append("cliente", reference()).append("profissional", nonblank(100)).append("observacoes", nonblank(2000)).append("criadoEm", date());
                required.addAll(List.of("cliente", "profissional", "observacoes", "criadoEm"));
            }
            case "receita" -> {
                props.append("cliente", reference()).append("prescritor", nonblank(100)).append("numero", nonblank(80))
                    .append("emitidaEm", date()).append("validadeAte", date()).append("itens", items(false));
                required.addAll(List.of("cliente", "prescritor", "numero", "emitidaEm", "validadeAte", "itens"));
            }
            case "dispensacao" -> {
                props.append("receita", reference()).append("venda", reference()).append("criadaEm", date()); required.addAll(List.of("receita", "venda", "criadaEm"));
            }
            case "configuracao" -> {
                props.put("_id", new Document("enum", List.of(1L))); props.append("nomeFarmacia", nonblank(100)); required.add("nomeFarmacia");
            }
            case "auditoria" -> {
                props.append("dataHora", date()).append("tipo", enumeration("ACESSO", "ALTERACAO", "PRECO"))
                    .append("usuario", string(50)).append("acao", string(500)).append("metodo", string(10)).append("caminho", string(200))
                    .append("status", integer(100, 599)).append("ip", string(45));
                required.addAll(List.of("dataHora", "tipo", "usuario", "acao", "metodo", "caminho", "status", "ip"));
            }
            default -> throw new IllegalArgumentException(collection);
        }
        Document validation = new Document("$jsonSchema", new Document("bsonType", "object").append("required", required).append("properties", props));
        if (collection.equals("receita")) {
            Document medicamentos = new Document("$map", new Document("input", "$itens").append("as", "item").append("in", "$$item.medicamento"));
            validation.append("$expr", new Document("$and", List.of(new Document("$gte", List.of("$validadeAte", "$emitidaEm")),
                new Document("$eq", List.of(new Document("$size", "$itens"), new Document("$size", new Document("$setUnion", List.of(medicamentos, List.of()))))))));
        }
        return validation;
    }
    private static Document items(boolean sale) {
        Document props = new Document("_id", new Document("bsonType", "long")).append("medicamento", reference()).append("quantidade", integer(1, sale ? Integer.MAX_VALUE : 9999));
        List<String> required = new ArrayList<>(List.of("_id", "medicamento", "quantidade"));
        if (sale) { props.append("precoUnitario", money()); required.add("precoUnitario"); }
        return new Document("bsonType", "array").append("items", new Document("bsonType", "object").append("required", required).append("properties", props));
    }
}
