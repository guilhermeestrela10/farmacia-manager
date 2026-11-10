package com.farmacia.farmacia.security;

import com.mongodb.DBRef;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Order(-50)
@ConditionalOnProperty(name = "app.importar-postgres", havingValue = "true")
public class ImportarPostgres implements ApplicationRunner {
    static final List<String> TABELAS = List.of("usuario", "categoria", "fabricante", "medicamento", "lote",
            "movimentacao_estoque", "venda", "venda_item", "cliente", "atendimento", "receita", "item_receita",
            "dispensacao", "configuracao", "auditoria");
    private static final String MARCADOR = "postgres-v1";
    private static final Map<String, Map<String, String>> REFERENCIAS = Map.ofEntries(
            Map.entry("medicamento", Map.of("categoria_id", "categoria", "fabricante_id", "fabricante")),
            Map.entry("lote", Map.of("medicamento_id", "medicamento")),
            Map.entry("movimentacao_estoque", Map.of("lote_id", "lote")),
            Map.entry("venda", Map.of("vendedor_id", "usuario", "cancelada_por_id", "usuario")),
            Map.entry("venda_item", Map.of("venda_id", "venda", "medicamento_id", "medicamento")),
            Map.entry("atendimento", Map.of("cliente_id", "cliente")),
            Map.entry("receita", Map.of("cliente_id", "cliente")),
            Map.entry("item_receita", Map.of("receita_id", "receita", "medicamento_id", "medicamento")),
            Map.entry("dispensacao", Map.of("receita_id", "receita", "venda_id", "venda")));
    private final MongoTemplate mongo;
    private final TransactionTemplate transacao;
    private final Environment ambiente;

    public ImportarPostgres(MongoTemplate mongo, MongoTransactionManager transactionManager, Environment ambiente) {
        this.mongo = mongo;
        this.transacao = new TransactionTemplate(transactionManager);
        this.ambiente = ambiente;
    }

    @Override public void run(ApplicationArguments args) {
        try {
            exigir("farmacia-manager".equals(mongo.getDb().getName()), "Banco MongoDB de importação não autorizado.");
            if (mongo.exists(Query.query(Criteria.where("_id").is(MARCADOR)), "_migrations")) return;
            validarDestino();
            String host = obrigatoria("MIGRATION_PG_HOST");
            String banco = obrigatoria("MIGRATION_PG_DATABASE");
            String porta = obrigatoria("MIGRATION_PG_PORT");
            validarOrigem(host, banco, porta);
            Properties propriedades = new Properties();
            propriedades.setProperty("user", obrigatoria("MIGRATION_PG_USER"));
            propriedades.setProperty("password", obrigatoria("MIGRATION_PG_PASSWORD"));
            propriedades.setProperty("sslmode", "require");
            propriedades.setProperty("connectTimeout", "15");
            propriedades.setProperty("socketTimeout", "60");
            propriedades.setProperty("ApplicationName", "FarmaciaMongoImport");
            String url = "jdbc:postgresql://" + host + ":" + porta + "/" + banco;
            try (Connection origem = abrir(url, propriedades)) {
                Map<String, List<Document>> dados = ler(origem);
                Map<String, List<Document>> documentos = converter(dados);
                String fingerprintOrigem = fingerprint(dados);
                if (!mongo.collectionExists("_migrations")) mongo.createCollection("_migrations");
                transacao.executeWithoutResult(status -> {
                    validarDestino();
                    Document contagens = new Document();
                    Document contagensDestino = new Document();
                    dados.forEach((tabela, linhas) -> contagens.put(tabela, linhas.size()));
                    documentos.forEach((colecao, linhas) -> contagensDestino.put(colecao, linhas.size()));
                    mongo.insert(new Document("_id", MARCADOR).append("sourceFingerprint", fingerprintOrigem)
                            .append("sourceCounts", contagens).append("targetCounts", contagensDestino)
                            .append("targetFingerprint", fingerprint(documentos)).append("importadaEm", new java.util.Date()), "_migrations");
                    mongo.remove(Query.query(Criteria.where("_id").is(1L)), "configuracao");
                    documentos.forEach((colecao, linhas) -> {
                        if (!linhas.isEmpty()) mongo.insert(linhas, colecao);
                        List<Document> gravados = mongo.find(new Query().with(Sort.by("_id")), Document.class, colecao);
                        exigir(fingerprint(linhas).equals(fingerprint(gravados)), "Reconciliação MongoDB divergente: " + colecao);
                    });
                    dados.forEach((tabela, linhas) -> {
                        long maximo = linhas.stream().mapToLong(l -> ((Number) l.get("id")).longValue()).max().orElse(0);
                        var resultado = mongo.updateFirst(Query.query(Criteria.where("_id").is(tabela)),
                                new Update().max("value", maximo), "_sequences");
                        exigir(resultado.getMatchedCount() == 1, "Contador MongoDB ausente: " + tabela);
                    });
                    // A second connection sees changes hidden by the original repeatable-read snapshot.
                    try (Connection atual = abrir(url, propriedades)) {
                        exigir(fingerprintOrigem.equals(fingerprint(ler(atual))),
                                "Origem mudou durante a importação; suspenda gravações e execute novamente.");
                    } catch (SQLException erro) {
                        throw new ImportacaoRecusada("Não foi possível reconferir a origem PostgreSQL.");
                    }
                });
            }
        } catch (ImportacaoRecusada erro) {
            throw new IllegalStateException(erro.getMessage());
        } catch (Exception erro) {
            // Driver exceptions can contain connection details or document values.
            throw new IllegalStateException("Importação PostgreSQL interrompida; confira o marcador antes de repetir ("
                    + erro.getClass().getSimpleName() + ").");
        }
    }

    private String obrigatoria(String nome) {
        String valor = ambiente.getProperty(nome);
        exigir(valor != null && !valor.isBlank(), "Variável de importação ausente: " + nome);
        return valor;
    }

    static void validarOrigem(String host, String banco, String porta) {
        exigir(host != null && host.matches("dpg-dbmg7vmk1f9s7385rp5g-a(?:\\.[a-z0-9-]+-postgres\\.render\\.com)?")
                && "farmacia_manager_demo_db".equals(banco), "Origem PostgreSQL não autorizada.");
        try {
            int numero = Integer.parseInt(porta);
            exigir(numero >= 1 && numero <= 65535 && porta.matches("[0-9]+"), "Porta PostgreSQL inválida.");
        } catch (NumberFormatException erro) {
            throw new ImportacaoRecusada("Porta PostgreSQL inválida.");
        }
    }

    private static Connection abrir(String url, Properties propriedades) throws SQLException {
        Connection conexao = DriverManager.getConnection(url, propriedades);
        try {
            conexao.setReadOnly(true);
            conexao.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            conexao.setAutoCommit(false);
            try (Statement sql = conexao.createStatement(); ResultSet resultado = sql.executeQuery(
                    "SELECT current_database(), current_setting('transaction_read_only'), current_setting('transaction_isolation')")) {
                exigir(resultado.next() && "farmacia_manager_demo_db".equals(resultado.getString(1))
                        && "on".equals(resultado.getString(2)) && "repeatable read".equals(resultado.getString(3)),
                        "A origem exige snapshot PostgreSQL somente leitura.");
            }
            return conexao;
        } catch (SQLException | RuntimeException erro) {
            conexao.close();
            throw erro;
        }
    }

    private static Map<String, List<Document>> ler(Connection conexao) throws SQLException {
        Map<String, List<Document>> dados = new LinkedHashMap<>();
        for (String tabela : TABELAS) {
            List<Document> linhas = new ArrayList<>();
            try (Statement sql = conexao.createStatement()) {
                sql.setQueryTimeout(60);
                try (ResultSet resultado = sql.executeQuery("SELECT * FROM public." + tabela + " ORDER BY id")) {
                    ResultSetMetaData meta = resultado.getMetaData();
                    while (resultado.next()) {
                        Document linha = new Document();
                        for (int coluna = 1; coluna <= meta.getColumnCount(); coluna++) {
                            Object valor = switch (meta.getColumnType(coluna)) {
                                case Types.DATE -> resultado.getObject(coluna, LocalDate.class);
                                case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> {
                                    Timestamp instante = resultado.getTimestamp(coluna);
                                    yield instante == null ? null : instante.toInstant();
                                }
                                default -> resultado.getObject(coluna);
                            };
                            linha.put(meta.getColumnName(coluna), valor);
                        }
                        linhas.add(linha);
                    }
                }
            }
            dados.put(tabela, linhas);
        }
        return dados;
    }

    private void validarDestino() {
        for (String colecao : mongo.getCollectionNames()) {
            if (colecao.startsWith("system.") || Set.of("_sequences", "_guards").contains(colecao)) continue;
            List<Document> existentes = mongo.find(new Query().limit(2), Document.class, colecao);
            if (existentes.isEmpty()) continue;
            boolean padrao = colecao.equals("configuracao") && existentes.size() == 1
                    && existentes.getFirst().equals(new Document("_id", 1L).append("nomeFarmacia", "Farmácias Ruanna").append("version", 0L));
            exigir(padrao, "Destino contém dados; importação recusada sem sobrescrever coleções.");
        }
    }

    static Map<String, List<Document>> converter(Map<String, List<Document>> dados) {
        exigir(dados.keySet().equals(new LinkedHashSet<>(TABELAS)), "Inventário PostgreSQL incompleto.");
        Map<String, Set<Long>> ids = new HashMap<>();
        for (String tabela : TABELAS) {
            Set<Long> encontrados = new HashSet<>();
            for (Document linha : dados.get(tabela)) {
                exigir(linha.get("id") instanceof Long && (Long) linha.get("id") > 0
                        && encontrados.add((Long) linha.get("id")), "IDs PostgreSQL inválidos: " + tabela);
            }
            ids.put(tabela, encontrados);
        }
        exigir(ids.get("configuracao").equals(Set.of(1L)), "Configuração PostgreSQL ausente ou inválida.");
        Map<String, List<Document>> documentos = new LinkedHashMap<>();
        for (String tabela : TABELAS) {
            List<Document> linhas = new ArrayList<>();
            for (Document linha : dados.get(tabela)) {
                Document documento = new Document("_id", linha.get("id"));
                if (!Set.of("venda_item", "item_receita").contains(tabela)) documento.put("version", 0L);
                for (var campo : linha.entrySet()) {
                    if (campo.getKey().equals("id")) continue;
                    String referencia = REFERENCIAS.getOrDefault(tabela, Map.of()).get(campo.getKey());
                    if (referencia != null) {
                        exigir(campo.getValue() == null || ids.get(referencia).contains(campo.getValue()),
                                "Referência PostgreSQL inválida: " + tabela);
                        documento.put(camel(campo.getKey().substring(0, campo.getKey().length() - 3)),
                                campo.getValue() == null ? null : new DBRef(referencia, campo.getValue()));
                    } else {
                        documento.put(camel(campo.getKey()), bson(campo.getValue()));
                    }
                }
                if (tabela.equals("movimentacao_estoque") && linha.get("venda_id") != null)
                    exigir(ids.get("venda").contains(linha.get("venda_id")), "Venda da movimentação PostgreSQL inválida.");
                linhas.add(documento);
            }
            documentos.put(tabela, linhas);
        }
        embutir(documentos, "venda", "venda_item", "venda");
        embutir(documentos, "receita", "item_receita", "receita");
        for (Document venda : documentos.get("venda")) {
            BigDecimal soma = BigDecimal.ZERO;
            for (Document item : venda.getList("itens", Document.class))
                soma = soma.add(((Decimal128) item.get("precoUnitario")).bigDecimalValue()
                        .multiply(BigDecimal.valueOf(((Number) item.get("quantidade")).longValue())));
            exigir(!venda.getList("itens", Document.class).isEmpty()
                    && venda.get("total") instanceof Decimal128
                    && soma.compareTo(((Decimal128) venda.get("total")).bigDecimalValue()) == 0,
                    "Total PostgreSQL divergente dos itens de venda.");
        }
        return documentos;
    }

    private static void embutir(Map<String, List<Document>> documentos, String pai, String itens, String campo) {
        Map<Long, Document> pais = new HashMap<>();
        documentos.get(pai).forEach(documento -> {
            documento.put("itens", new ArrayList<Document>());
            pais.put((Long) documento.get("_id"), documento);
        });
        for (Document item : documentos.remove(itens)) {
            DBRef referencia = (DBRef) item.remove(campo);
            exigir(referencia != null && pais.containsKey(referencia.getId()), "Item PostgreSQL sem documento pai.");
            pais.get(referencia.getId()).getList("itens", Document.class).add(item);
        }
    }

    private static String camel(String nome) {
        StringBuilder convertido = new StringBuilder();
        boolean maiuscula = false;
        for (char letra : nome.toCharArray()) {
            if (letra == '_') maiuscula = true;
            else { convertido.append(maiuscula ? Character.toUpperCase(letra) : letra); maiuscula = false; }
        }
        return convertido.toString();
    }

    private static Object bson(Object valor) {
        if (valor instanceof BigDecimal decimal) return new Decimal128(decimal);
        if (valor instanceof LocalDate data) return java.util.Date.from(data.atStartOfDay(ZoneId.systemDefault()).toInstant());
        if (valor instanceof Instant instante) return java.util.Date.from(instante);
        return valor;
    }

    static String fingerprint(Object dados) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            acumular(digest, dados);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("SHA-256 indisponível.");
        }
    }

    private static void acumular(MessageDigest digest, Object valor) {
        if (valor instanceof DBRef ref) {
            acumular(digest, new Document("$ref", ref.getCollectionName()).append("$id", ref.getId()));
        } else if (valor instanceof Map<?, ?> mapa) {
            texto(digest, "map");
            texto(digest, Integer.toString(mapa.size()));
            mapa.keySet().stream().map(Object::toString).sorted().forEach(chave -> {
                texto(digest, chave); acumular(digest, mapa.get(chave));
            });
        } else if (valor instanceof List<?> lista) {
            texto(digest, "list"); texto(digest, Integer.toString(lista.size()));
            lista.forEach(item -> acumular(digest, item));
        } else {
            texto(digest, valor == null ? "null" : valor instanceof java.util.Date ? "date" : valor.getClass().getName());
            texto(digest, valor == null ? "" : valor instanceof java.util.Date data ? Long.toString(data.getTime()) : valor.toString());
        }
    }

    private static void texto(MessageDigest digest, String texto) {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) throw new ImportacaoRecusada(mensagem);
    }
    private static class ImportacaoRecusada extends IllegalStateException {
        ImportacaoRecusada(String mensagem) { super(mensagem); }
    }
}
