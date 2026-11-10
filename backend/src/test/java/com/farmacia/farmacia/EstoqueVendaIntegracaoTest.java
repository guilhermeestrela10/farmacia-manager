package com.farmacia.farmacia;

import static org.junit.jupiter.api.Assertions.*;
import com.farmacia.farmacia.dto.*;
import com.farmacia.farmacia.model.*;
import com.farmacia.farmacia.repository.*;
import com.farmacia.farmacia.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "farmacia.test.mongo-uri", matches = "mongodb://127\\.0\\.0\\.1:55433/\\?replicaSet=farmacia-teste")
class EstoqueVendaIntegracaoTest {
    @Autowired CategoriaRepository categorias;
    @Autowired FabricanteRepository fabricantes;
    @Autowired MedicamentoRepository medicamentos;
    @Autowired UsuarioRepository usuarios;
    @Autowired LoteRepository lotes;
    @Autowired VendaRepository vendas;
    @Autowired MovimentacaoEstoqueRepository movimentos;
    @Autowired EstoqueService estoque;
    @Autowired VendaService vendaService;
    @Autowired UsuarioService usuarioService;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;

    private record Cenario(Usuario usuario, Medicamento medicamento) {}

    private Cenario cenario() {
        String sufixo = UUID.randomUUID().toString();
        var categoria = categorias.saveAndFlush(new Categoria("Categoria " + sufixo));
        var fabricante = fabricantes.saveAndFlush(new Fabricante("Fabricante " + sufixo));
        var med = medicamentos.saveAndFlush(new Medicamento("Medicamento fictício", null, categoria, fabricante,
                TipoMedicamento.GENERICO, new BigDecimal("10.00"), 0));
        var usuario = usuarios.saveAndFlush(new Usuario("Pessoa fictícia", "teste-" + sufixo,
                encoder.encode("Ficticia123!"), Perfil.VENDEDOR));
        return new Cenario(usuario, med);
    }

    private EntradaEstoqueRequest entrada(Cenario c, String numero, int quantidade) {
        return new EntradaEstoqueRequest(c.medicamento().getId(), numero, LocalDate.now().plusDays(30), quantidade, "Teste", "Teste");
    }

    private VendaRequest pedido(Medicamento med, int quantidade) {
        return new VendaRequest(List.of(new ItemVendaRequest(med.getId(), quantidade, med.getPreco())), FormaPagamento.PIX);
    }

    private Lote lote(Cenario c, String numero, int dias, int quantidade) {
        var lote = new Lote(c.medicamento(), numero, LocalDate.now().plusDays(dias));
        lote.adicionar(quantidade);
        lote = lotes.saveAndFlush(lote);
        movimentos.saveAndFlush(new MovimentacaoEstoque(lote, TipoMovimentacao.ENTRADA, quantidade, "Fixture fictícia", "Teste"));
        return lote;
    }

    private long saldo(Lote lote) { return lotes.findById(lote.getId()).orElseThrow().getQuantidadeAtual(); }

    private List<Integer> simultaneas(Supplier<?> primeira, Supplier<?> segunda) throws Exception {
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tarefas = List.of(primeira, segunda).stream().map(acao -> executor.submit(() -> {
                assertTrue(inicio.await(10, TimeUnit.SECONDS));
                try { acao.get(); return 200; }
                catch (RuntimeException ex) { return ConflitoMongoTeste.status(ex); }
            })).toList();
            inicio.countDown();
            return List.of(tarefas.get(0).get(20, TimeUnit.SECONDS), tarefas.get(1).get(20, TimeUnit.SECONDS));
        }
    }

    @Test
    void entradasConcorrentesNoMesmoLoteSomamSemDuplicar() throws Exception {
        var c = cenario();
        var resultados = simultaneas(
                () -> estoque.registrarEntrada(entrada(c, "MESMO", 5)),
                () -> estoque.registrarEntrada(entrada(c, "MESMO", 7)));
        assertTrue(resultados.stream().allMatch(status -> status == 200 || status == 409));
        assertTrue(resultados.contains(200));
        if (resultados.get(0) == 409) estoque.registrarEntrada(entrada(c, "MESMO", 5));
        if (resultados.get(1) == 409) estoque.registrarEntrada(entrada(c, "MESMO", 7));
        var lista = estoque.listarLotes(c.medicamento().getId());
        assertEquals(1, lista.size());
        assertEquals(12, lista.getFirst().quantidadeAtual());
    }

    @Test
    void duasVendasNaoConsomemAMesmaUnidade() throws Exception {
        var c = cenario();
        estoque.registrarEntrada(entrada(c, "UNICO", 1));
        var resultados = simultaneas(() -> vendaService.registrar(pedido(c.medicamento(), 1), c.usuario().getLogin()),
                () -> vendaService.registrar(pedido(c.medicamento(), 1), c.usuario().getLogin()));
        assertEquals(1, resultados.stream().filter(status -> status == 200).count());
        assertEquals(1, resultados.stream().filter(status -> status == 409).count());
        assertEquals(0, estoque.consultarSaldo(c.medicamento().getId()).saldoTotal());
    }

    @Test
    void fefoIgnoraVencidosECancelamentoDevolveAosMesmosLotes() {
        var c = cenario();
        var vencido = lote(c, "VENCIDO", -1, 9);
        var primeiro = lote(c, "PRIMEIRO", 1, 5);
        var segundo = lote(c, "SEGUNDO", 2, 5);
        var venda = vendaService.registrar(pedido(c.medicamento(), 7), c.usuario().getLogin());
        assertEquals(9, saldo(vencido));
        assertEquals(0, saldo(primeiro));
        assertEquals(3, saldo(segundo));
        vendaService.cancelar(venda.id(), "Teste de estorno", c.usuario().getLogin(), true);
        assertEquals(5, saldo(primeiro));
        assertEquals(5, saldo(segundo));
        assertEquals(9, saldo(vencido));
        var erro = assertThrows(ResponseStatusException.class, () -> vendaService.cancelar(venda.id(), "Repetido", c.usuario().getLogin(), true));
        assertEquals(409, erro.getStatusCode().value());
    }

    @Test
    void cancelamentosConcorrentesEstornamCadaSaidaSomenteUmaVez() throws Exception {
        var c = cenario();
        var lote = lote(c, "CANCELAMENTO", 10, 5);
        var venda = vendaService.registrar(pedido(c.medicamento(), 2), c.usuario().getLogin());
        var resultados = simultaneas(
                () -> vendaService.cancelar(venda.id(), "Primeiro", c.usuario().getLogin(), true),
                () -> vendaService.cancelar(venda.id(), "Segundo", c.usuario().getLogin(), true));
        assertEquals(1, resultados.stream().filter(status -> status == 200).count());
        assertEquals(1, resultados.stream().filter(status -> status == 409).count());
        assertEquals(5, saldo(lote));
        assertEquals(StatusVenda.CANCELADA, vendas.findById(venda.id()).orElseThrow().getStatus());
        var estornos = movimentos.findByVendaIdAndTipo(venda.id(), TipoMovimentacao.ESTORNO);
        assertEquals(1, estornos.size());
        assertEquals(2, estornos.getFirst().getQuantidade());
    }

    @Test
    void vendaComItemSemEstoqueReverteVendaEOutrasBaixas() {
        var c = cenario();
        var outro = cenario();
        var primeiro = lote(c, "DISPONIVEL", 1, 5);
        long antes = vendas.count();
        var request = new VendaRequest(List.of(new ItemVendaRequest(c.medicamento().getId(), 1, null),
                new ItemVendaRequest(outro.medicamento().getId(), 1, null)), FormaPagamento.PIX);
        assertThrows(ResponseStatusException.class, () -> vendaService.registrar(request, c.usuario().getLogin()));
        assertEquals(5, saldo(primeiro));
        assertEquals(antes, vendas.count());
    }

    @Test
    void redefinicaoConcorrenteNaoReativaUsuario() throws Exception {
        var c = cenario();
        var resultados = simultaneas(() -> {
            usuarioService.redefinirSenha(c.usuario().getId(), "NovaFicticia123!");
            return true;
        }, () -> usuarioService.desativar(c.usuario().getId(), "outro-admin"));
        assertTrue(resultados.stream().allMatch(status -> status == 200 || status == 409));
        assertTrue(resultados.contains(200));
        if (resultados.get(0) == 409) usuarioService.redefinirSenha(c.usuario().getId(), "NovaFicticia123!");
        if (resultados.get(1) == 409) usuarioService.desativar(c.usuario().getId(), "outro-admin");
        var usuario = usuarios.findById(c.usuario().getId()).orElseThrow();
        assertFalse(usuario.isAtivo());
        assertTrue(encoder.matches("NovaFicticia123!", usuario.getSenhaHash()));
    }

    @Test
    void administradoresConcorrentesNaoDesativamOUltimo() throws Exception {
        String sufixo = UUID.randomUUID().toString();
        var a = usuarios.saveAndFlush(new Usuario("Admin A", "a-" + sufixo, "hash-ficticio", Perfil.ADMIN));
        var b = usuarios.saveAndFlush(new Usuario("Admin B", "b-" + sufixo, "hash-ficticio", Perfil.ADMIN));
        var resultados = simultaneas(() -> usuarioService.desativar(b.getId(), a.getLogin()),
                () -> usuarioService.desativar(a.getId(), b.getLogin()));
        assertEquals(1, resultados.stream().filter(status -> status == 200).count());
        assertEquals(1, resultados.stream().filter(status -> status == 409).count());
        assertEquals(1, usuarios.countByPerfilAndAtivoTrue(Perfil.ADMIN));
    }
}
